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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A comanda impressa de ponta a ponta: a estação lista o que falta, pega a comanda (uma vez só),
 * o item vai para "em preparo", a via "CANCELADO" do item cancelado depois, a reimpressão, os
 * pedidos parados na confirmação, os papéis e a separação entre restaurantes.
 */
@IntegrationTest
class PrintFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** O dono, a cozinha (Kenji), o garçom (Carlos), a mesa 04 e dois produtos. */
    record Restaurant(String owner, String kitchen, String waiter, String qr04, String guioza, String sunomono) {
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
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
        // O MockMvc chega como 127.0.0.1: esta é "a rede do salão".
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr04 = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        return new Restaurant(owner, staff(owner, establishment, "Kenji", Role.KITCHEN),
                staff(owner, establishment, "Carlos", Role.STAFF), qr04,
                product(owner, category, "Guioza", 2990), product(owner, category, "Sunomono", 1990));
    }

    /** Cadastra a pessoa pela API e emite o token dela direto (o login por PIN precisa de aparelho). */
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

    private static List<String> list(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    /** Um celular na mesa, com a presença confirmada, faz um pedido. Devolve o pedido (JSON). */
    private MvcTestResult order(Restaurant r, String items) {
        Cookie phone = mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/presence").cookie(phone).exchange();
        MvcTestResult placed = mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/orders").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[" + items + "]}").exchange();
        assertThat(placed).hasStatus(HttpStatus.CREATED);
        return placed;
    }

    private static String line(String productId, int quantity) {
        return "{\"productId\":\"%s\",\"quantity\":%d}".formatted(productId, quantity);
    }

    private MvcTestResult step(String token, String itemId, String step) {
        return send("POST", token, "/api/v1/order-items/" + itemId + "/" + step, null);
    }

    private String status(String itemId) {
        return jdbc.queryForObject("SELECT status FROM order_item WHERE id = ?::uuid", String.class, itemId);
    }

    private MvcTestResult pending(String token, String kind) {
        return send("GET", token, "/api/v1/print-jobs?kind=" + kind, null);
    }

    private MvcTestResult claim(String token, String orderId, String kind) {
        return send("POST", token, "/api/v1/print-jobs/claim",
                "{\"orderId\":\"%s\",\"kind\":\"%s\"}".formatted(orderId, kind));
    }

    private List<String> itemIds(String orderId) {
        return jdbc.queryForList("SELECT id::text FROM order_item WHERE order_id = ?::uuid ORDER BY position",
                String.class, orderId);
    }

    @Test
    void theStationPrintsEachOrderOnceAndTheItemsGoToPreparation() throws Exception {
        String orderId = json(order(sakura, line(sakura.guioza(), 2) + "," + line(sakura.sunomono(), 1)), "$.id");

        MvcTestResult jobs = pending(sakura.kitchen(), "ORDER");
        assertThat(jobs).hasStatusOk();
        assertThat(list(jobs, "$.content[*].orderId")).containsExactly(orderId);
        assertThat(json(jobs, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(json(jobs, "$.content[0].sectorName")).isEqualTo("Salão");
        assertThat(json(jobs, "$.content[0].orderNumber")).isEqualTo("1");
        assertThat(list(jobs, "$.content[0].items[*].productName")).containsExactly("Guioza", "Sunomono");
        assertThat((List<Integer>) JsonPath.read(jobs.getResponse().getContentAsString(), "$.content[0].items[*].quantity"))
                .containsExactly(2, 1);

        // A estação pega a comanda: recebe os itens, e eles vão para "em preparo".
        MvcTestResult claimed = claim(sakura.kitchen(), orderId, "ORDER");
        assertThat(claimed).hasStatusOk();
        assertThat(list(claimed, "$.items[*].productName")).containsExactly("Guioza", "Sunomono");
        assertThat(itemIds(orderId)).allSatisfy(id -> assertThat(status(id)).isEqualTo("PREPARING"));

        // Uma segunda estação (ou um F5) não imprime de novo.
        assertThat(list(claim(sakura.waiter(), orderId, "ORDER"), "$.items")).isEmpty();
        assertThat(list(pending(sakura.kitchen(), "ORDER"), "$.content")).isEmpty();

        // Ela aparece entre as impressas, para reimprimir.
        MvcTestResult printed = send("GET", sakura.kitchen(), "/api/v1/print-jobs/printed", null);
        assertThat(list(printed, "$.content[*].orderId")).containsExactly(orderId);
        assertThat(list(printed, "$.content[0].items[*].productName")).containsExactly("Guioza", "Sunomono");
    }

    @Test
    void anItemCanceledAfterPrintingGetsTheCanceledTicket() throws Exception {
        String orderId = json(order(sakura, line(sakura.guioza(), 1) + "," + line(sakura.sunomono(), 1)), "$.id");
        claim(sakura.kitchen(), orderId, "ORDER");
        String sunomono = itemIds(orderId).get(1);

        // Antes de cancelar, não há via "CANCELADO".
        assertThat(list(pending(sakura.kitchen(), "CANCELLATION"), "$.content")).isEmpty();
        assertThat(send("POST", sakura.waiter(), "/api/v1/order-items/" + sunomono + "/cancel",
                "{\"reason\":\"Acabou o pepino\"}")).hasStatus(HttpStatus.NO_CONTENT);

        MvcTestResult cancellations = pending(sakura.kitchen(), "CANCELLATION");
        assertThat(list(cancellations, "$.content[0].items[*].productName")).containsExactly("Sunomono");
        assertThat(list(cancellations, "$.content[0].items[*].cancelReason")).containsExactly("Acabou o pepino");

        MvcTestResult claimed = claim(sakura.kitchen(), orderId, "CANCELLATION");
        assertThat(list(claimed, "$.items[*].cancelReason")).containsExactly("Acabou o pepino");
        assertThat(list(claim(sakura.kitchen(), orderId, "CANCELLATION"), "$.items")).isEmpty();
        assertThat(list(pending(sakura.kitchen(), "CANCELLATION"), "$.content")).isEmpty();
    }

    @Test
    void anItemCanceledBeforePrintingNeverPrints() throws Exception {
        String orderId = json(order(sakura, line(sakura.guioza(), 1) + "," + line(sakura.sunomono(), 1)), "$.id");
        String sunomono = itemIds(orderId).get(1);
        send("POST", sakura.waiter(), "/api/v1/order-items/" + sunomono + "/cancel", "{\"reason\":\"Acabou\"}");

        MvcTestResult claimed = claim(sakura.kitchen(), orderId, "ORDER");
        assertThat(list(claimed, "$.items[*].productName")).containsExactly("Guioza");
        assertThat(list(pending(sakura.kitchen(), "CANCELLATION"), "$.content")).isEmpty();
    }

    @Test
    void anOrderWaitingForTheWaiterIsNotPrintedYet() throws Exception {
        send("PUT", sakura.owner(), "/api/v1/order-settings",
                "{\"orderingEnabled\":true,\"approvalRequired\":true}");
        String orderId = json(order(sakura, line(sakura.guioza(), 1)), "$.id");

        assertThat(list(pending(sakura.kitchen(), "ORDER"), "$.content")).isEmpty();
        assertThat(list(claim(sakura.kitchen(), orderId, "ORDER"), "$.items")).isEmpty();
        assertThat(status(itemIds(orderId).getFirst())).isEqualTo("PENDING");
    }

    @Test
    void anotherRestaurantNeitherSeesNorPrintsTheOrders() throws Exception {
        Restaurant cantina = newRestaurant("Cantina");
        String orderId = json(order(sakura, line(sakura.guioza(), 1)), "$.id");

        assertThat(list(pending(cantina.owner(), "ORDER"), "$.content")).isEmpty();
        assertThat(claim(cantina.kitchen(), orderId, "ORDER")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(list(send("GET", cantina.owner(), "/api/v1/print-jobs/printed", null), "$.content")).isEmpty();
        // E nada mudou no pedido do Sakura.
        assertThat(status(itemIds(orderId).getFirst())).isEqualTo("PENDING");
        assertThat(list(pending(sakura.kitchen(), "ORDER"), "$.content[*].orderId")).containsExactly(orderId);
    }

    @Test
    void theWholeTeamCanRunTheStationButNotAnonymously() {
        assertThat(pending(sakura.waiter(), "ORDER")).hasStatusOk();
        assertThat(pending(sakura.kitchen(), "ORDER")).hasStatusOk();
        assertThat(pending(null, "ORDER")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(pending(sakura.owner(), "NADA")).hasStatus(HttpStatus.BAD_REQUEST);
    }
}
