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
 * A cozinha e o garçom de ponta a ponta: a fila, em preparo, pronto, entregue, o cancelamento com
 * motivo e a confirmação do garçom, com os papéis e a separação entre restaurantes.
 */
@IntegrationTest
class KitchenFlowTest {

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

    @Test
    void theKitchenAndTheWaiterTakeTheOrderToTheTable() throws Exception {
        MvcTestResult placed = order(sakura, line(sakura.guioza(), 2) + "," + line(sakura.sunomono(), 1));
        String guioza = json(placed, "$.items[0].id");
        String sunomono = json(placed, "$.items[1].id");

        MvcTestResult queue = send("GET", sakura.kitchen(), "/api/v1/order-items", null);
        assertThat(queue).hasStatusOk();
        assertThat(list(queue, "$.content[*].productName")).containsExactly("Guioza", "Sunomono");
        assertThat(json(queue, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(json(queue, "$.content[0].sectorName")).isEqualTo("Salão");
        assertThat(json(queue, "$.content[0].orderNumber")).isEqualTo("1");
        assertThat(json(queue, "$.content[0].quantity")).isEqualTo("2");

        assertThat(step(sakura.kitchen(), guioza, "start")).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(step(sakura.kitchen(), guioza, "ready")).hasStatus(HttpStatus.NO_CONTENT);
        // A bebida que ninguém prepara pula direto para pronto.
        assertThat(step(sakura.kitchen(), sunomono, "ready")).hasStatus(HttpStatus.NO_CONTENT);
        MvcTestResult ready = send("GET", sakura.waiter(), "/api/v1/order-items?status=READY", null);
        assertThat(list(ready, "$.content[*].id")).containsExactly(guioza, sunomono);

        // Entregar é de quem atende o salão, não da cozinha.
        assertThat(step(sakura.kitchen(), guioza, "deliver")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(step(sakura.waiter(), guioza, "deliver")).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(step(sakura.waiter(), guioza, "deliver")).hasStatus(HttpStatus.CONFLICT);

        assertThat(status(guioza)).isEqualTo("DELIVERED");
        assertThat(jdbc.queryForObject("SELECT delivered_at IS NOT NULL AND delivered_by IS NOT NULL "
                + "AND preparing_at IS NOT NULL FROM order_item WHERE id = ?::uuid", Boolean.class, guioza)).isTrue();
        assertThat(list(send("GET", sakura.kitchen(), "/api/v1/order-items", null), "$.content[*].id"))
                .containsExactly(sunomono);
        assertThat(send("GET", sakura.kitchen(), "/api/v1/order-items?status=DELIVERED", null))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theStaffCancelsWithAReasonAndTheTableSeesIt() throws Exception {
        MvcTestResult placed = order(sakura, line(sakura.sunomono(), 1));
        String sunomono = json(placed, "$.items[0].id");
        step(sakura.kitchen(), sunomono, "start");

        assertThat(send("POST", sakura.kitchen(), "/api/v1/order-items/" + sunomono + "/cancel",
                "{\"reason\":\"  \"}")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("POST", sakura.kitchen(), "/api/v1/order-items/" + sunomono + "/cancel",
                "{\"reason\":\"Acabou o pepino japonês\"}")).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM order_item WHERE id = ?::uuid", String.class,
                sunomono)).isEqualTo("Acabou o pepino japonês");
        assertThat(status(sunomono)).isEqualTo("CANCELED");
        assertThat(step(sakura.kitchen(), sunomono, "ready")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void aWaitingOrderGoesToTheKitchenOnlyAfterTheWaiter() throws Exception {
        send("PUT", sakura.owner(), "/api/v1/order-settings",
                "{\"orderingEnabled\":true,\"approvalRequired\":true}");
        MvcTestResult first = order(sakura, line(sakura.guioza(), 1) + "," + line(sakura.sunomono(), 2));
        MvcTestResult second = order(sakura, line(sakura.guioza(), 3));
        String firstId = json(first, "$.id");
        String secondId = json(second, "$.id");
        assertThat(json(first, "$.status")).isEqualTo("PENDING_APPROVAL");

        assertThat(list(send("GET", sakura.kitchen(), "/api/v1/order-items", null), "$.content")).isEmpty();
        assertThat(step(sakura.kitchen(), json(first, "$.items[0].id"), "start")).hasStatus(HttpStatus.CONFLICT);
        // A cozinha não decide o que entra.
        assertThat(send("GET", sakura.kitchen(), "/api/v1/orders/awaiting-approval", null))
                .hasStatus(HttpStatus.FORBIDDEN);
        MvcTestResult waiting = send("GET", sakura.waiter(), "/api/v1/orders/awaiting-approval", null);
        assertThat(list(waiting, "$.content[*].id")).containsExactly(firstId, secondId);
        assertThat(json(waiting, "$.content[0].totalCents")).isEqualTo(String.valueOf(2990 + 2 * 1990));
        assertThat(json(waiting, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(list(waiting, "$.content[0].items[*].productName")).containsExactly("Guioza", "Sunomono");

        assertThat(send("POST", sakura.waiter(), "/api/v1/orders/" + firstId + "/approve", null))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("POST", sakura.waiter(), "/api/v1/orders/" + firstId + "/reject", null))
                .hasStatus(HttpStatus.CONFLICT);
        assertThat(send("POST", sakura.waiter(), "/api/v1/orders/" + secondId + "/reject", null))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(list(send("GET", sakura.kitchen(), "/api/v1/order-items", null), "$.content[*].productName"))
                .containsExactly("Guioza", "Sunomono");
        assertThat(list(send("GET", sakura.waiter(), "/api/v1/orders/awaiting-approval", null), "$.content"))
                .isEmpty();
        assertThat(jdbc.queryForList("SELECT cancel_reason FROM order_item WHERE order_id = ?::uuid", String.class,
                secondId)).containsExactly("Pedido recusado pelo garçom");
    }

    @Test
    void anotherRestaurantSeesNothingAndChangesNothing() throws Exception {
        send("PUT", sakura.owner(), "/api/v1/order-settings",
                "{\"orderingEnabled\":true,\"approvalRequired\":true}");
        MvcTestResult waiting = order(sakura, line(sakura.guioza(), 1));
        String orderId = json(waiting, "$.id");
        String itemId = json(waiting, "$.items[0].id");
        Restaurant other = newRestaurant("Outro Bar");

        assertThat(send("POST", other.waiter(), "/api/v1/orders/" + orderId + "/approve", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", other.waiter(), "/api/v1/orders/" + orderId + "/reject", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(list(send("GET", other.waiter(), "/api/v1/orders/awaiting-approval", null), "$.content"))
                .isEmpty();

        send("POST", sakura.waiter(), "/api/v1/orders/" + orderId + "/approve", null);
        assertThat(list(send("GET", other.kitchen(), "/api/v1/order-items", null), "$.content")).isEmpty();
        for (String s : List.of("start", "ready", "deliver")) {
            assertThat(step(other.owner(), itemId, s)).hasStatus(HttpStatus.NOT_FOUND);
        }
        assertThat(send("POST", other.owner(), "/api/v1/order-items/" + itemId + "/cancel",
                "{\"reason\":\"Trote\"}")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(status(itemId)).isEqualTo("PENDING");
    }
}
