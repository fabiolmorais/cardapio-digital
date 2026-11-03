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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O pedido pelo celular de ponta a ponta: o carrinho vira pedido, os celulares da mesa caem na
 * mesma comanda, o preço fica o do momento, e as travas e os limites valem.
 */
@IntegrationTest
class OrderFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    record Restaurant(String owner, UUID userId, UUID establishmentId, String qr04, String qr05, String category,
                      String guioza, String sunomono) {

        String orders(String qr) {
            return "/api/v1/public/tables/" + qr + "/orders";
        }
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
        // O MockMvc chega como 127.0.0.1: esta é "a rede do salão".
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr04 = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String qr05 = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"05\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        return new Restaurant(owner, UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")), qr04, qr05, category,
                product(owner, category, "Guioza", 2990), product(owner, category, "Sunomono", 1990));
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
            case "PATCH" -> mvc.patch().uri(uri);
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

    /** Um celular na mesa, com a presença confirmada pela rede do salão. */
    private Cookie phone(String qr) {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + qr + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + qr + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult order(Restaurant r, String qr, Cookie phone, String items) {
        return mvc.post().uri(r.orders(qr)).cookie(phone).contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[" + items + "]}").exchange();
    }

    private static String line(String productId, int quantity) {
        return "{\"productId\":\"%s\",\"quantity\":%d}".formatted(productId, quantity);
    }

    @Test
    void threePhonesShareOneTab() throws Exception {
        Cookie ana = phone(sakura.qr04());
        Cookie bia = phone(sakura.qr04());
        Cookie caio = phone(sakura.qr04());

        MvcTestResult first = order(sakura, sakura.qr04(), ana, line(sakura.guioza(), 1));
        assertThat(first).hasStatus(HttpStatus.CREATED);
        assertThat(json(first, "$.number")).isEqualTo("1");
        assertThat(json(first, "$.status")).isEqualTo("SENT");
        assertThat(order(sakura, sakura.qr04(), bia, line(sakura.sunomono(), 2))).hasStatus(HttpStatus.CREATED);
        assertThat(order(sakura, sakura.qr04(), caio,
                line(sakura.guioza(), 1) + "," + "{\"productId\":\"%s\",\"quantity\":1,\"note\":\"sem pepino\"}"
                        .formatted(sakura.sunomono()))).hasStatus(HttpStatus.CREATED);

        MvcTestResult tab = mvc.get().uri(sakura.orders(sakura.qr04())).cookie(bia).exchange();
        assertThat(tab).bodyJson().extractingPath("$.orders[*].number").asArray().containsExactly(1, 2, 3);
        assertThat(tab).bodyJson().extractingPath("$.orders[*].mine").asArray().containsExactly(false, true, false);
        assertThat(json(tab, "$.totalCents")).isEqualTo(String.valueOf(2990 + 2 * 1990 + 2990 + 1990));
        assertThat(json(tab, "$.orders[2].items[1].note")).isEqualTo("sem pepino");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tab WHERE establishment_id = ?", Integer.class,
                sakura.establishmentId())).isEqualTo(1);
    }

    @Test
    void twoFirstOrdersAtTheSameTimeOpenOnlyOneTab() throws Exception {
        List<Cookie> phones = List.of(phone(sakura.qr04()), phone(sakura.qr04()), phone(sakura.qr04()));

        List<Integer> statuses = together(phones.stream().<Callable<Integer>>map(p ->
                () -> order(sakura, sakura.qr04(), p, line(sakura.guioza(), 1)).getResponse().getStatus()).toList());

        assertThat(statuses).containsOnly(201);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tab WHERE establishment_id = ?", Integer.class,
                sakura.establishmentId())).isEqualTo(1);
        // E três números diferentes, mesmo ao mesmo tempo.
        assertThat(jdbc.queryForList("SELECT number FROM customer_order WHERE establishment_id = ? ORDER BY number",
                Integer.class, sakura.establishmentId())).containsExactly(1, 2, 3);
    }

    @Test
    void thePriceStaysTheOneOfTheMoment() throws Exception {
        Cookie ana = phone(sakura.qr04());
        order(sakura, sakura.qr04(), ana, line(sakura.guioza(), 2));

        send("PUT", sakura.owner(), "/api/v1/products/" + sakura.guioza(), """
                {"categoryId":"%s","name":"Guioza especial","priceCents":4990,"featured":false,"available":true}
                """.formatted(sakura.category()));

        MvcTestResult tab = mvc.get().uri(sakura.orders(sakura.qr04())).cookie(ana).exchange();
        assertThat(json(tab, "$.orders[0].items[0].productName")).isEqualTo("Guioza");
        assertThat(json(tab, "$.orders[0].items[0].unitPriceCents")).isEqualTo("2990");
    }

    @Test
    void aSoldOutItemOrOneFromAnotherRestaurantIsRefused() throws Exception {
        Cookie ana = phone(sakura.qr04());
        send("PATCH", sakura.owner(), "/api/v1/products/" + sakura.sunomono() + "/availability",
                "{\"available\":false}");
        Restaurant other = newRestaurant("Outro Bar");

        MvcTestResult soldOut = order(sakura, sakura.qr04(), ana,
                line(sakura.guioza(), 1) + "," + line(sakura.sunomono(), 1));
        assertThat(soldOut).hasStatus(HttpStatus.CONFLICT);
        assertThat(soldOut.getResponse().getContentAsString()).contains("Sunomono");
        assertThat(order(sakura, sakura.qr04(), ana, line(other.guioza(), 1))).hasStatus(HttpStatus.CONFLICT);
        assertThat(order(sakura, sakura.qr04(), ana, line(sakura.guioza(), 0))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theSettingsDecideWhereTheOrderGoes() throws Exception {
        Cookie ana = phone(sakura.qr04());
        assertThat(json(order(sakura, sakura.qr04(), ana, line(sakura.sunomono(), 11)), "$.status"))
                .isEqualTo("PENDING_APPROVAL"); // 11 itens: passa do limite de 10

        assertThat(send("PUT", sakura.owner(), "/api/v1/order-settings",
                "{\"orderingEnabled\":false,\"approvalRequired\":false}")).hasStatusOk();
        assertThat(order(sakura, sakura.qr04(), ana, line(sakura.guioza(), 1))).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(mvc.get().uri(sakura.orders(sakura.qr04())).cookie(ana).exchange(), "$.orderingEnabled"))
                .isEqualTo("false");
        assertThat(json(send("GET", sakura.owner(), "/api/v1/order-settings", null), "$.maxItems"))
                .isEqualTo("null"); // salvou sem limite
    }

    @Test
    void theCustomerCancelsHisOwnQueuedItem() throws Exception {
        Cookie ana = phone(sakura.qr04());
        Cookie bia = phone(sakura.qr04());
        Cookie mesa05 = phone(sakura.qr05());
        String item = json(order(sakura, sakura.qr04(), ana, line(sakura.guioza(), 1)), "$.items[0].id");
        String cancel = "/api/v1/public/tables/%s/order-items/" + item;

        assertThat(mvc.delete().uri(cancel.formatted(sakura.qr04())).cookie(bia).exchange())
                .hasStatus(HttpStatus.CONFLICT); // é da Ana
        assertThat(mvc.delete().uri(cancel.formatted(sakura.qr05())).cookie(mesa05).exchange())
                .hasStatus(HttpStatus.NOT_FOUND); // nem é desta mesa
        assertThat(mvc.delete().uri(cancel.formatted(sakura.qr04())).cookie(ana).exchange())
                .hasStatus(HttpStatus.NO_CONTENT);

        MvcTestResult tab = mvc.get().uri(sakura.orders(sakura.qr04())).cookie(ana).exchange();
        assertThat(json(tab, "$.orders[0].items[0].status")).isEqualTo("CANCELED");
        assertThat(json(tab, "$.orders[0].items[0].cancelReason")).isEqualTo("Cancelado pelo cliente");
        assertThat(json(tab, "$.totalCents")).isEqualTo("0");
    }

    @Test
    void withoutPresenceOrSendingWithoutStopTheOrderIsRefused() throws Exception {
        Cookie outside = mvc.post().uri("/api/v1/public/tables/" + sakura.qr04() + "/session")
                .with(r -> {
                    r.setRemoteAddr("177.10.20.30");
                    return r;
                }).exchange().getResponse().getCookie("table_session");
        assertThat(order(sakura, sakura.qr04(), outside, line(sakura.guioza(), 1))).hasStatus(HttpStatus.FORBIDDEN);

        Cookie ana = phone(sakura.qr04());
        for (int i = 0; i < 3; i++) {
            assertThat(order(sakura, sakura.qr04(), ana, line(sakura.guioza(), 1))).hasStatus(HttpStatus.CREATED);
        }
        MvcTestResult fourth = order(sakura, sakura.qr04(), ana, line(sakura.guioza(), 1));
        assertThat(fourth).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(fourth.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
    }

    /** Separação entre restaurantes: os números, as comandas e as configurações de um não se misturam. */
    @Test
    void eachRestaurantHasItsOwnNumbersTabsAndSettings() throws Exception {
        Restaurant other = newRestaurant("Outro Bar");
        order(sakura, sakura.qr04(), phone(sakura.qr04()), line(sakura.guioza(), 1));
        order(sakura, sakura.qr04(), phone(sakura.qr04()), line(sakura.guioza(), 1));

        assertThat(json(order(other, other.qr04(), phone(other.qr04()), line(other.guioza(), 1)), "$.number"))
                .isEqualTo("1");
        send("PUT", sakura.owner(), "/api/v1/order-settings", "{\"orderingEnabled\":false,\"approvalRequired\":true}");
        assertThat(json(send("GET", other.owner(), "/api/v1/order-settings", null), "$.orderingEnabled"))
                .isEqualTo("true");
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();
        assertThat(send("PUT", waiter, "/api/v1/order-settings",
                "{\"orderingEnabled\":true,\"approvalRequired\":false}")).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Dispara as tarefas juntas (um portão que abre para todas) e devolve os status HTTP. */
    private static List<Integer> together(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<Integer>> futures = tasks.stream().map(task -> pool.submit(() -> {
                gate.await();
                return task.call();
            })).toList();
            gate.countDown();
            return futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        } finally {
            pool.shutdownNow();
        }
    }
}
