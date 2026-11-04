package br.com.cardapio.adapter.in.web.rodizio;

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

/** O rodízio na mesa, de ponta a ponta: o pedido da mesa, a ativação, as rodadas e a conta. */
@IntegrationTest
class RodizioTableFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** O dono, o garçom, a cozinha, a mesa 04, o Japonês (com o Guioza) e o Sunomono, que fica de fora. */
    record Restaurant(String owner, String waiter, String kitchen, String tableId, String qr04, String rodizio,
                      String guioza, String sunomono) {
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
        MvcTestResult table = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector));
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        String guioza = product(owner, category, "Guioza", 2990);
        String sunomono = product(owner, category, "Sunomono", 1990);
        // Só o preço padrão: o preço por dia e horário já tem teste nos serviços, com o relógio parado.
        MvcTestResult rodizio = send("POST", owner, "/api/v1/rodizios", """
                {"name":"Rodízio Japonês","type":"A_LA_MINUTE","rules":{},
                 "tiers":[{"name":"Adulto","defaultPriceCents":12990,"prices":[]},
                          {"name":"Criança","defaultPriceCents":6490,"prices":[]}],
                 "productIds":["%s"]}
                """.formatted(guioza));
        assertThat(rodizio).hasStatus(HttpStatus.CREATED);
        return new Restaurant(owner, staff(owner, establishment, "Carlos", Role.STAFF),
                staff(owner, establishment, "Kenji", Role.KITCHEN), json(table, "$.id"),
                json(table, "$.publicToken"), json(rodizio, "$.id"), guioza, sunomono);
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

    private static List<Object> list(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    /** Um celular na mesa, com a presença confirmada pela rede do salão. */
    private Cookie phone(Restaurant r) {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult fromPhone(Restaurant r, Cookie phone, String method, String path, String body) {
        var request = "POST".equals(method)
                ? mvc.post().uri("/api/v1/public/tables/" + r.qr04() + path)
                : mvc.get().uri("/api/v1/public/tables/" + r.qr04() + path);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return request.cookie(phone).exchange();
    }

    private static String items(String productId, int quantity) {
        return "{\"items\":[{\"productId\":\"%s\",\"quantity\":%d}]}".formatted(productId, quantity);
    }

    private MvcTestResult activate(String token, Restaurant table, String rodizioId, int adults, int children) {
        return send("POST", token, "/api/v1/tables/" + table.tableId() + "/rodizio", """
                {"rodizioId":"%s","guests":[{"tierName":"Adulto","quantity":%d},{"tierName":"Criança","quantity":%d}]}
                """.formatted(rodizioId, adults, children));
    }

    @Test
    void theTableAsksTheWaiterActivatesAndTheRoundsGoAtZero() throws Exception {
        Cookie phone = phone(sakura);

        // "Escolha o rodízio", com o preço por pessoa de agora, e o pedido ao garçom.
        MvcTestResult options = fromPhone(sakura, phone, "GET", "/rodizios", null);
        assertThat(options).bodyJson().extractingPath("$.content[0].tiers[*].priceCents").asArray()
                .containsExactly(12990, 6490);
        MvcTestResult request = fromPhone(sakura, phone, "POST", "/calls", "{\"type\":\"RODIZIO\"}");
        assertThat(request).hasStatus(HttpStatus.CREATED);
        // Na fila da equipe, o chamado traz a mesa: é por ela que o garçom ativa.
        MvcTestResult queue = send("GET", sakura.waiter(), "/api/v1/calls", null);
        assertThat(queue).bodyJson().extractingPath("$.content[0].type").isEqualTo("RODIZIO");
        assertThat(queue).bodyJson().extractingPath("$.content[0].tableId").isEqualTo(sakura.tableId());
        // Sem o rodízio ativo, a rodada não vai.
        assertThat(fromPhone(sakura, phone, "POST", "/rounds", items(sakura.guioza(), 1)))
                .hasStatus(HttpStatus.CONFLICT);

        MvcTestResult activated = activate(sakura.waiter(), sakura, sakura.rodizio(), 3, 1);
        assertThat(activated).hasStatus(HttpStatus.CREATED);
        assertThat(activated).bodyJson().extractingPath("$.people").isEqualTo(4);
        assertThat(activated).bodyJson().extractingPath("$.totalCents").isEqualTo(3 * 12990 + 6490);
        assertThat(jdbc.queryForObject("SELECT status FROM call WHERE id = ?::uuid", String.class,
                json(request, "$.id"))).isEqualTo("DONE");
        assertThat(activate(sakura.waiter(), sakura, sakura.rodizio(), 2, 0)).hasStatus(HttpStatus.CONFLICT);

        MvcTestResult round = fromPhone(sakura, phone, "POST", "/rounds", items(sakura.guioza(), 4));
        assertThat(round).hasStatus(HttpStatus.CREATED);
        assertThat(round).bodyJson().extractingPath("$.items[0].unitPriceCents").isEqualTo(0);
        assertThat(round).bodyJson().extractingPath("$.items[0].rodizio").isEqualTo(true);
        MvcTestResult notIncluded = fromPhone(sakura, phone, "POST", "/rounds", items(sakura.sunomono(), 1));
        assertThat(notIncluded).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(notIncluded, "$.detail")).isEqualTo("Sunomono não faz parte do rodízio. Peça pelo carrinho.");
        assertThat(fromPhone(sakura, phone, "POST", "/orders", items(sakura.sunomono(), 1)))
                .hasStatus(HttpStatus.CREATED);

        // A cozinha recebe a rodada como qualquer pedido.
        assertThat(send("GET", sakura.kitchen(), "/api/v1/order-items", null)).bodyJson()
                .extractingPath("$.content[*].productName").asArray().contains("Guioza");
        MvcTestResult orders = fromPhone(sakura, phone, "GET", "/orders", null);
        assertThat(orders).bodyJson().extractingPath("$.rodizio.name").isEqualTo("Rodízio Japonês");
        assertThat(orders).bodyJson().extractingPath("$.rodizioProductIds").asArray().containsExactly(sakura.guioza());
        assertThat(orders).bodyJson().extractingPath("$.totalCents").isEqualTo(3 * 12990 + 6490 + 1990);
    }

    @Test
    void someoneArrivesAndTheBillIsPeopleTimesPricePlusTheExtras() throws Exception {
        Cookie phone = phone(sakura);
        assertThat(activate(sakura.waiter(), sakura, sakura.rodizio(), 2, 0)).hasStatus(HttpStatus.CREATED);
        MvcTestResult drink = fromPhone(sakura, phone, "POST", "/orders", items(sakura.sunomono(), 1));
        send("POST", sakura.waiter(), "/api/v1/order-items/" + json(drink, "$.items[0].id") + "/deliver", null);

        MvcTestResult changed = send("PUT", sakura.waiter(), "/api/v1/tables/" + sakura.tableId() + "/rodizio/guests",
                "{\"guests\":[{\"tierName\":\"Adulto\",\"quantity\":2},{\"tierName\":\"Criança\",\"quantity\":1}]}");
        assertThat(changed).hasStatusOk();
        assertThat(changed).bodyJson().extractingPath("$.people").isEqualTo(3);

        int expected = 2 * 12990 + 6490 + 1990;
        MvcTestResult open = send("GET", sakura.waiter(), "/api/v1/tabs/open", null);
        assertThat(open).bodyJson().extractingPath("$.content[0].totalCents").isEqualTo(expected);
        String tabId = json(open, "$.content[0].id");
        MvcTestResult tab = send("GET", sakura.waiter(), "/api/v1/tabs/" + tabId, null);
        assertThat(tab).bodyJson().extractingPath("$.rodizio.guests[*].quantity").asArray().containsExactly(2, 1);
        assertThat(tab).bodyJson().extractingPath("$.totalCents").isEqualTo(expected);

        MvcTestResult closed = send("POST", sakura.waiter(), "/api/v1/tabs/" + tabId + "/close",
                "{\"paymentMethod\":\"PIX\"}");
        assertThat(closed).hasStatusOk();
        assertThat(closed).bodyJson().extractingPath("$.totalCents").isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT total_cents FROM tab WHERE id = ?::uuid", Integer.class, tabId))
                .isEqualTo(expected);
        // Depois da conta, a mesa não está mais no rodízio.
        assertThat(send("PUT", sakura.waiter(), "/api/v1/tables/" + sakura.tableId() + "/rodizio/guests",
                "{\"guests\":[{\"tierName\":\"Adulto\",\"quantity\":1}]}")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void anotherRestaurantGets404AndSeesNothing() throws Exception {
        Restaurant other = newRestaurant("Outro Sushi");

        assertThat(activate(other.waiter(), sakura, other.rodizio(), 2, 0)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(activate(sakura.waiter(), sakura, other.rodizio(), 2, 0)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(activate(sakura.waiter(), sakura, sakura.rodizio(), 2, 0)).hasStatus(HttpStatus.CREATED);
        assertThat(send("PUT", other.waiter(), "/api/v1/tables/" + sakura.tableId() + "/rodizio/guests",
                "{\"guests\":[{\"tierName\":\"Adulto\",\"quantity\":9}]}")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.waiter(), "/api/v1/rodizio-quotes", null)).bodyJson()
                .extractingPath("$.content[*].id").asArray().containsExactly(other.rodizio());
        assertThat(jdbc.queryForObject("SELECT sum(quantity) FROM tab_rodizio_guest WHERE establishment_id = "
                + "(SELECT establishment_id FROM dining_table WHERE id = ?::uuid)", Integer.class, sakura.tableId()))
                .isEqualTo(2);
    }

    @Test
    void theWrongDataTheWrongRoleAndBothTypesInTheQuotes() throws Exception {
        assertThat(activate(sakura.waiter(), sakura, sakura.rodizio(), 0, 0)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("POST", sakura.waiter(), "/api/v1/tables/" + sakura.tableId() + "/rodizio",
                "{\"rodizioId\":\"%s\",\"guests\":[]}".formatted(sakura.rodizio()))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(activate(sakura.kitchen(), sakura, sakura.rodizio(), 2, 0)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", sakura.kitchen(), "/api/v1/rodizio-quotes", null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(activate(null, sakura, sakura.rodizio(), 2, 0)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/public/tables/" + sakura.qr04() + "/rodizios").exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);

        String passagem = json(send("POST", sakura.owner(), "/api/v1/rodizios", """
                {"name":"Churrasco","type":"PASSAGEM","rules":{},
                 "tiers":[{"name":"Adulto","defaultPriceCents":15990,"prices":[]}],"productIds":[]}
                """), "$.id");
        assertThat(passagem).isNotBlank();
        // O garçom vê os dois tipos (o de passagem funciona na mesa desde o Guia 27).
        assertThat(list(send("GET", sakura.waiter(), "/api/v1/rodizio-quotes", null), "$.content[*].type"))
                .containsExactly("PASSAGEM", "A_LA_MINUTE");
    }
}
