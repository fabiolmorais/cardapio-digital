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
 * A comanda do lado da equipe, de ponta a ponta: as abertas, o detalhe, o fechamento depois do
 * pagamento (com as sessões e os chamados da mesa) e a separação entre restaurantes.
 */
@IntegrationTest
class TabFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** O dono, a cozinha, o garçom, a mesa 04 e dois produtos. */
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

    /** Um celular na mesa, com a presença confirmada pela rede do salão. */
    private Cookie phone(Restaurant r) {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult order(Restaurant r, Cookie phone, String productId, int quantity) {
        MvcTestResult placed = mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/orders").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"%s\",\"quantity\":%d}]}".formatted(productId, quantity))
                .exchange();
        assertThat(placed).hasStatus(HttpStatus.CREATED);
        return placed;
    }

    private void deliver(Restaurant r, MvcTestResult placed) throws Exception {
        for (String item : list(placed, "$.items[*].id")) {
            assertThat(send("POST", r.waiter(), "/api/v1/order-items/" + item + "/deliver", null))
                    .hasStatus(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    void theWaiterClosesTheTabAfterPaymentAndTheTableStartsOver() throws Exception {
        Cookie ana = phone(sakura);
        Cookie bia = phone(sakura);
        MvcTestResult first = order(sakura, ana, sakura.guioza(), 2);
        MvcTestResult second = order(sakura, bia, sakura.sunomono(), 1);

        MvcTestResult open = send("GET", sakura.waiter(), "/api/v1/tabs/open", null);
        assertThat(open).hasStatusOk();
        assertThat(json(open, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(json(open, "$.content[0].totalCents")).isEqualTo(String.valueOf(2 * 2990 + 1990));
        assertThat(json(open, "$.content[0].itemsOnTheWay")).isEqualTo("2");
        String tabId = json(open, "$.content[0].id");

        // Com itens a caminho, não fecha: a mensagem diz o que falta.
        MvcTestResult blocked = send("POST", sakura.waiter(), "/api/v1/tabs/" + tabId + "/close",
                "{\"paymentMethod\":\"PIX\"}");
        assertThat(blocked).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(blocked, "$.detail")).contains("2× Guioza", "1× Sunomono");

        deliver(sakura, first);
        deliver(sakura, second);
        assertThat(mvc.post().uri("/api/v1/public/tables/" + sakura.qr04() + "/calls").cookie(ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"BILL\",\"paymentMethod\":\"CARD\"}")
                .exchange()).hasStatus(HttpStatus.CREATED);
        MvcTestResult detail = send("GET", sakura.waiter(), "/api/v1/tabs/" + tabId, null);
        assertThat(json(detail, "$.billRequestedWith")).isEqualTo("CARD");
        assertThat(json(detail, "$.orders[*].number")).isEqualTo("[1,2]");

        assertThat(send("POST", sakura.waiter(), "/api/v1/tabs/" + tabId + "/close", "{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        MvcTestResult closed = send("POST", sakura.waiter(), "/api/v1/tabs/" + tabId + "/close",
                "{\"paymentMethod\":\"CARD\"}");
        assertThat(closed).hasStatusOk();
        assertThat(json(closed, "$.status")).isEqualTo("CLOSED");
        assertThat(json(closed, "$.totalCents")).isEqualTo(String.valueOf(2 * 2990 + 1990));
        assertThat(json(closed, "$.paymentMethod")).isEqualTo("CARD");
        assertThat(send("POST", sakura.waiter(), "/api/v1/tabs/" + tabId + "/close",
                "{\"paymentMethod\":\"CARD\"}")).hasStatus(HttpStatus.CONFLICT);

        assertThat(list(send("GET", sakura.waiter(), "/api/v1/tabs/open", null), "$.content")).isEmpty();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM call
                 WHERE table_id = (SELECT table_id FROM tab WHERE id = ?::uuid) AND status IN ('PENDING', 'IN_PROGRESS')
                """, Integer.class, tabId)).isZero();
        // Os celulares da mesa saíram: a próxima visita começa do zero, numa comanda nova.
        assertThat(mvc.get().uri("/api/v1/public/tables/" + sakura.qr04() + "/orders").cookie(ana).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        MvcTestResult next = order(sakura, phone(sakura), sakura.guioza(), 1);
        assertThat(json(next, "$.number")).isEqualTo("3");
        assertThat(list(send("GET", sakura.waiter(), "/api/v1/tabs/open", null), "$.content[*].id"))
                .singleElement().isNotEqualTo(tabId);
        // A fechada continua consultável, com o que foi cobrado.
        assertThat(json(send("GET", sakura.owner(), "/api/v1/tabs/" + tabId, null), "$.status"))
                .isEqualTo("CLOSED");
    }

    @Test
    void theKitchenDoesNotCloseTabs() {
        assertThat(send("GET", sakura.kitchen(), "/api/v1/tabs/open", null)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void anotherRestaurantSeesNothingAndClosesNothing() throws Exception {
        order(sakura, phone(sakura), sakura.guioza(), 1);
        String tabId = json(send("GET", sakura.waiter(), "/api/v1/tabs/open", null), "$.content[0].id");
        Restaurant other = newRestaurant("Outro Bar");

        assertThat(list(send("GET", other.waiter(), "/api/v1/tabs/open", null), "$.content")).isEmpty();
        assertThat(send("GET", other.owner(), "/api/v1/tabs/" + tabId, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", other.owner(), "/api/v1/tabs/" + tabId + "/close", "{\"paymentMethod\":\"PIX\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("SELECT status FROM tab WHERE id = ?::uuid", String.class, tabId))
                .isEqualTo("OPEN");
    }
}
