package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O pedido lançado pelo garçom de ponta a ponta (ADR 0011): a configuração, a comanda, a trava de
 * duplicados com dois garçons ao mesmo tempo, o que a mesa vê e o isolamento entre restaurantes.
 */
@IntegrationTest
class StaffOrderFlowTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String SETTINGS_ON = """
            {"orderingEnabled":true,"approvalRequired":false,"staffOrderingEnabled":true}
            """;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    record Restaurant(String owner, UUID establishmentId, String joao, String ana, String kitchen, String qr,
                      String tableId, String category, String chopp, String guioza) {

        String orders() {
            return "/api/v1/tables/" + tableId + "/orders";
        }

        String sheet() {
            return "/api/v1/tables/" + tableId + "/staff-order";
        }
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
        assertThat(send("PUT", sakura.owner(), "/api/v1/order-settings", SETTINGS_ON)).hasStatusOk();
    }

    private Restaurant newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = json(login, "$.accessToken");
        UUID establishment = UUID.fromString(json(login, "$.establishment.id"));
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        MvcTestResult table = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector));
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Bar\"}"), "$.id");
        return new Restaurant(owner, establishment, staff(owner, establishment, "João", Role.STAFF),
                staff(owner, establishment, "Ana", Role.STAFF), staff(owner, establishment, "Rita", Role.KITCHEN),
                json(table, "$.publicToken"), json(table, "$.id"), category,
                product(owner, category, "Chopp", 1200), product(owner, category, "Guioza", 2990));
    }

    private String staff(String owner, UUID establishment, String name, Role role) throws Exception {
        MvcTestResult added = send("POST", owner, "/api/v1/team-members",
                "{\"name\":\"%s\",\"role\":\"%s\",\"pin\":\"258014\"}".formatted(name, role));
        assertThat(added).hasStatus(HttpStatus.CREATED);
        UUID userId = jdbc.queryForObject("SELECT user_id FROM membership WHERE id = ?::uuid", UUID.class,
                json(added, "$.id"));
        return tokenIssuer.issue(new TokenSubject(new UserId(userId), new EstablishmentId(establishment), role))
                .value();
    }

    private String product(String owner, String category, String name, long price) throws Exception {
        return json(send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"%s","priceCents":%d,"featured":false,"available":true}
                """.formatted(category, name, price)), "$.id");
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
            case "PUT" -> mvc.put().uri(uri);
            default -> mvc.get().uri(uri);
        };
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), path));
    }

    private static List<Object> list(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private static String body(UUID request, String product, String... confirmed) {
        String list = String.join(",", List.of(confirmed).stream().map(c -> "\"" + c + "\"").toList());
        return """
                {"clientRequestId":"%s","round":false,"confirmedDuplicates":[%s],
                 "items":[{"productId":"%s","quantity":2,"note":"sem gelo"}]}
                """.formatted(request, list, product);
    }

    private MvcTestResult place(String token, String product, String... confirmed) {
        return send("POST", token, sakura.orders(), body(UUID.randomUUID(), product, confirmed));
    }

    private Cookie phone() {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + sakura.qr() + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + sakura.qr() + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult phoneOrders(Cookie phone) {
        return mvc.get().uri("/api/v1/public/tables/" + sakura.qr() + "/orders").cookie(phone).exchange();
    }

    private long orders() {
        return jdbc.queryForObject("SELECT count(*) FROM customer_order WHERE establishment_id = ?", Long.class,
                sakura.establishmentId());
    }

    @Test
    void itStartsOffAndOnlyTheOwnerOrManagerTurnsItOn() throws Exception {
        Restaurant nova = newRestaurant("Casa Nova");

        assertThat(json(send("GET", nova.owner(), "/api/v1/order-settings", null), "$.staffOrderingEnabled"))
                .isEqualTo("false");
        assertThat(json(send("GET", nova.joao(), nova.sheet(), null), "$.orderingEnabled")).isEqualTo("false");
        assertThat(send("POST", nova.joao(), nova.orders(), body(UUID.randomUUID(), nova.chopp())))
                .hasStatus(HttpStatus.CONFLICT);
        assertThat(send("PUT", nova.joao(), "/api/v1/order-settings", SETTINGS_ON)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void theWaiterOrdersForATableWithNoPhoneAndTheTeamSeesWho() throws Exception {
        MvcTestResult placed = place(sakura.joao(), sakura.chopp());

        assertThat(placed).hasStatus(HttpStatus.CREATED);
        MvcTestResult tab = send("GET", sakura.owner(), "/api/v1/tabs/" + json(placed, "$.tabId"), null);
        assertThat(json(tab, "$.orders[0].status")).isEqualTo("SENT");
        assertThat(json(tab, "$.orders[0].byStaff")).isEqualTo("true");
        assertThat(json(tab, "$.orders[0].placedByName")).isEqualTo("João");
        assertThat(json(tab, "$.orders[0].items[0].note")).isEqualTo("sem gelo");
        assertThat(json(tab, "$.totalCents")).isEqualTo("2400");
        assertThat(list(send("GET", sakura.kitchen(), "/api/v1/order-items", null), "$.content[*].productName"))
                .containsExactly("Chopp");
    }

    @Test
    void theSecondWaiterGetsTheWarningAndCanConfirm() throws Exception {
        place(sakura.joao(), sakura.chopp());

        MvcTestResult warned = place(sakura.ana(), sakura.chopp());

        assertThat(warned).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(warned, "$.duplicates[0].productName")).isEqualTo("Chopp");
        assertThat(json(warned, "$.duplicates[0].placedByName")).isEqualTo("João");
        assertThat(json(warned, "$.duplicates[0].quantity")).isEqualTo("2");
        assertThat(orders()).isEqualTo(1);

        MvcTestResult confirmed = place(sakura.ana(), sakura.chopp(), sakura.chopp());
        assertThat(confirmed).hasStatus(HttpStatus.CREATED);
        assertThat(jdbc.queryForObject("SELECT duplicate_confirmed FROM customer_order WHERE id = ?::uuid",
                Boolean.class, json(confirmed, "$.id"))).isTrue();
    }

    @Test
    void anOrderFromThePhoneAlsoWarnsTheWaiter() throws Exception {
        Cookie bia = phone();
        assertThat(mvc.post().uri("/api/v1/public/tables/" + sakura.qr() + "/orders").cookie(bia)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"%s\",\"quantity\":1}]}".formatted(sakura.chopp())).exchange())
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult warned = place(sakura.joao(), sakura.chopp());

        assertThat(warned).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(warned, "$.duplicates[0].byStaff")).isEqualTo("false");
        assertThat(list(send("GET", sakura.joao(), sakura.sheet(), null), "$.alreadyOrdered[*].productName"))
                .containsExactly("Chopp");
    }

    @Test
    void twoWaitersAtTheSameInstantDoNotBothGetThrough() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<Integer>> sent = List.of(sakura.joao(), sakura.ana()).stream().map(token -> pool.submit(() -> {
                gate.await();
                return place(token, sakura.chopp()).getResponse().getStatus();
            })).toList();
            gate.countDown();

            assertThat(List.of(sent.get(0).get(), sent.get(1).get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(orders()).isEqualTo(1);
    }

    @Test
    void repeatingARequestDoesNotCreateAnotherOrder() throws Exception {
        UUID request = UUID.randomUUID();
        MvcTestResult first = send("POST", sakura.joao(), sakura.orders(), body(request, sakura.chopp()));

        MvcTestResult repeated = send("POST", sakura.joao(), sakura.orders(), body(request, sakura.chopp()));

        assertThat(repeated).hasStatus(HttpStatus.CREATED);
        assertThat(json(repeated, "$.id")).isEqualTo(json(first, "$.id"));
        assertThat(orders()).isEqualTo(1);
    }

    @Test
    void theTableSeesTheOrderButCannotCancelIt() throws Exception {
        place(sakura.joao(), sakura.chopp());
        // O garçom lançou uma hora antes de o cliente escanear: ainda é da visita.
        jdbc.update("UPDATE customer_order SET created_at = now() - interval '1 hour' WHERE establishment_id = ?",
                sakura.establishmentId());
        Cookie bia = phone();

        MvcTestResult seen = phoneOrders(bia);

        assertThat(json(seen, "$.orders[0].byStaff")).isEqualTo("true");
        assertThat(json(seen, "$.orders[0].mine")).isEqualTo("false");
        assertThat(json(seen, "$.orders[0].phone")).isEqualTo("0");
        assertThat(json(seen, "$.totalCents")).isEqualTo("2400");
        assertThat(mvc.delete().uri("/api/v1/public/tables/" + sakura.qr() + "/order-items/"
                + json(seen, "$.orders[0].items[0].id")).cookie(bia).exchange()).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void anOldStaffOrderOnAForgottenTabStaysHiddenFromTheTable() throws Exception {
        place(sakura.joao(), sakura.chopp());
        jdbc.update("UPDATE customer_order SET created_at = now() - interval '3 hours' WHERE establishment_id = ?",
                sakura.establishmentId());

        assertThat(list(phoneOrders(phone()), "$.orders")).isEmpty();
    }

    @Test
    void theWaiterSendsRodizioRoundsWithoutTheDuplicateWarning() throws Exception {
        String rodizio = json(send("POST", sakura.owner(), "/api/v1/rodizios", """
                {"name":"Rodízio Japonês","type":"A_LA_MINUTE","rules":{},
                 "tiers":[{"name":"Adulto","defaultPriceCents":12990,"prices":[]}],"productIds":["%s"]}
                """.formatted(sakura.guioza())), "$.id");
        assertThat(send("POST", sakura.joao(), "/api/v1/tables/" + sakura.tableId() + "/rodizio", """
                {"rodizioId":"%s","guests":[{"tierName":"Adulto","quantity":2}]}
                """.formatted(rodizio))).hasStatus(HttpStatus.CREATED);
        MvcTestResult sheet = send("GET", sakura.joao(), sakura.sheet(), null);
        assertThat(json(sheet, "$.rodizio.name")).isEqualTo("Rodízio Japonês");
        assertThat(list(sheet, "$.rodizio.productIds")).containsExactly(sakura.guioza());
        String round = """
                {"clientRequestId":"%s","round":true,"items":[{"productId":"%s","quantity":3}]}
                """;

        assertThat(send("POST", sakura.joao(), sakura.orders(), round.formatted(UUID.randomUUID(), sakura.guioza())))
                .hasStatus(HttpStatus.CREATED);
        assertThat(send("POST", sakura.ana(), sakura.orders(), round.formatted(UUID.randomUUID(), sakura.guioza())))
                .hasStatus(HttpStatus.CREATED);
        assertThat(send("POST", sakura.joao(), sakura.orders(), round.formatted(UUID.randomUUID(), sakura.chopp())))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void theStaffMenuHasNoMediaAndTheKitchenCannotOrder() throws Exception {
        MvcTestResult menu = send("GET", sakura.joao(), "/api/v1/staff-menu", null);

        assertThat(menu).hasStatusOk();
        assertThat(list(menu, "$.categories[0].products[*].name")).containsExactlyInAnyOrder("Chopp", "Guioza");
        assertThat(list(menu, "$.categories[0].products[0].media")).isEmpty();
        assertThat(send("GET", sakura.kitchen(), "/api/v1/staff-menu", null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", sakura.kitchen(), sakura.sheet(), null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(place(sakura.kitchen(), sakura.chopp())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(place(null, sakura.chopp())).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anotherRestaurantGets404AndSeesNothing() throws Exception {
        Restaurant outro = newRestaurant("Outro Bar");
        assertThat(send("PUT", outro.owner(), "/api/v1/order-settings", SETTINGS_ON)).hasStatusOk();

        assertThat(send("GET", outro.joao(), sakura.sheet(), null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", outro.joao(), sakura.orders(), body(UUID.randomUUID(), outro.chopp())))
                .hasStatus(HttpStatus.NOT_FOUND);
        // O produto de outro restaurante, na mesa dele mesmo, não existe.
        assertThat(send("POST", outro.joao(), outro.orders(), body(UUID.randomUUID(), sakura.chopp())))
                .hasStatus(HttpStatus.CONFLICT);
        // O mesmo código de envio de outro restaurante não devolve o pedido daqui.
        UUID request = UUID.randomUUID();
        MvcTestResult here = send("POST", sakura.joao(), sakura.orders(), body(request, sakura.chopp()));
        MvcTestResult there = send("POST", outro.joao(), outro.orders(), body(request, outro.chopp()));
        assertThat(there).hasStatus(HttpStatus.CREATED);
        assertThat(json(there, "$.id")).isNotEqualTo(json(here, "$.id"));
        assertThat(list(send("GET", outro.joao(), "/api/v1/staff-menu", null), "$.categories[0].products[*].id"))
                .doesNotContain(sakura.chopp(), sakura.guioza());
    }
}
