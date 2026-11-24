package br.com.cardapio.adapter.in.web.rating;

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
 * A avaliação do atendimento (RF40, etapa C5), de ponta a ponta: quando a mesa pode avaliar, uma por
 * celular, o convite ao Google, o alerta de nota baixa, as avaliações do dia, a média do garçom e o
 * acesso de outro restaurante.
 */
@IntegrationTest
class RatingFlowTest {

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

    private String tableUri(Restaurant r, String path) {
        return "/api/v1/public/tables/" + r.qr04() + path;
    }

    private MvcTestResult offer(Restaurant r, Cookie phone) {
        return mvc.get().uri(tableUri(r, "/rating")).cookie(phone).exchange();
    }

    private MvcTestResult rate(Restaurant r, Cookie phone, String body) {
        return mvc.post().uri(tableUri(r, "/rating")).cookie(phone).contentType(MediaType.APPLICATION_JSON)
                .content(body).exchange();
    }

    private void bill(Restaurant r, Cookie phone) {
        assertThat(mvc.post().uri(tableUri(r, "/calls")).cookie(phone).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BILL\",\"paymentMethod\":\"PIX\"}").exchange()).hasStatus(HttpStatus.CREATED);
    }

    private String openTab(Restaurant r) throws Exception {
        return json(send("GET", r.waiter(), "/api/v1/tabs/open", null), "$.content[0].id");
    }

    private Restaurant sakura;
    private Cookie ana;

    @BeforeEach
    void sakuraWithAServedTable() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
        ana = phone(sakura);
        deliver(sakura, order(sakura, ana, sakura.guioza(), 2));
    }

    @Test
    void beforeTheBillThePhoneCannotRate() throws Exception {
        assertThat(json(offer(sakura, ana), "$.available")).isEqualTo("false");
        assertThat(rate(sakura, ana, "{\"stars\":5}")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void afterTheBillThePhoneRatesOnceAndAHighNoteInvitesToGoogle() throws Exception {
        bill(sakura, ana);
        MvcTestResult offered = offer(sakura, ana);
        assertThat(json(offered, "$.available")).isEqualTo("true");
        assertThat(json(offered, "$.waiterName")).isEqualTo("Carlos");

        MvcTestResult rated = rate(sakura, ana, """
                {"stars":5,"tags":["FOOD","FRIENDLINESS"],"comment":"  O Carlos explicou cada peça!  "}
                """);
        assertThat(rated).hasStatus(HttpStatus.CREATED);
        assertThat(json(rated, "$.googleInvite")).isEqualTo("true");
        assertThat(rate(sakura, ana, "{\"stars\":1}")).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(offer(sakura, ana), "$.available")).isEqualTo("false");
        assertThat(json(offer(sakura, ana), "$.stars")).isEqualTo("5");
        assertThat(mvc.post().uri(tableUri(sakura, "/rating/google")).cookie(ana).exchange())
                .hasStatus(HttpStatus.NO_CONTENT);

        MvcTestResult today = send("GET", sakura.owner(), "/api/v1/ratings/today", null);
        assertThat(json(today, "$.count")).isEqualTo("1");
        assertThat(json(today, "$.average")).isEqualTo("5.0");
        assertThat(json(today, "$.high")).isEqualTo("1");
        assertThat(json(today, "$.googleClicks")).isEqualTo("1");
        MvcTestResult list = send("GET", sakura.owner(), "/api/v1/ratings", null);
        assertThat(json(list, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(json(list, "$.content[0].waiterName")).isEqualTo("Carlos");
        assertThat(json(list, "$.content[0].comment")).isEqualTo("O Carlos explicou cada peça!");
        assertThat(list(list, "$.content[0].tags")).isEqualTo(List.of("FRIENDLINESS", "FOOD"));
        assertThat(json(list, "$.content[0].google")).isEqualTo("true");
    }

    @Test
    void aLowNoteWarnsTheManagerUntilSomeoneResolvesIt() throws Exception {
        bill(sakura, ana);
        assertThat(rate(sakura, ana, "{\"stars\":2,\"tags\":[\"SPEED\"],\"comment\":\"A conta demorou\"}"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult alerts = send("GET", sakura.owner(), "/api/v1/ratings/alerts", null);
        assertThat(list(alerts, "$[*].stars")).isEqualTo(List.of(2));
        assertThat(send("GET", sakura.waiter(), "/api/v1/ratings/alerts", null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", sakura.waiter(), "/api/v1/ratings", null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri(tableUri(sakura, "/rating/google")).cookie(ana).exchange())
                .hasStatus(HttpStatus.CONFLICT);

        String id = json(alerts, "$[0].id");
        assertThat(send("POST", sakura.owner(), "/api/v1/ratings/" + id + "/resolve", null))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(list(send("GET", sakura.owner(), "/api/v1/ratings/alerts", null), "$[*]")).isEmpty();
        assertThat(json(send("GET", sakura.owner(), "/api/v1/ratings?onlyLow=true", null), "$.totalElements"))
                .isEqualTo("1");
        assertThat(json(send("GET", sakura.owner(), "/api/v1/ratings/today", null), "$.low")).isEqualTo("1");
    }

    @Test
    void afterTheTabClosesThePhoneStillRatesForHalfAnHour() throws Exception {
        Cookie bruno = phone(sakura);
        assertThat(send("POST", sakura.waiter(), "/api/v1/tabs/" + openTab(sakura) + "/close",
                "{\"paymentMethod\":\"CARD\"}")).hasStatusOk();

        assertThat(json(offer(sakura, ana), "$.available")).isEqualTo("true");
        assertThat(rate(sakura, ana, "{\"stars\":4}")).hasStatus(HttpStatus.CREATED);
        jdbc.update("UPDATE table_session SET ended_at = now() - interval '40 minutes' WHERE ended_at IS NOT NULL");
        assertThat(json(offer(sakura, bruno), "$.available")).isEqualTo("false");
        assertThat(rate(sakura, bruno, "{\"stars\":4}")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void theWaiterSeesTheAverageOnlyWhenTheRestaurantAllows() throws Exception {
        bill(sakura, ana);
        rate(sakura, ana, "{\"stars\":4}");

        assertThat(json(send("GET", sakura.waiter(), "/api/v1/ratings/mine", null), "$.visible")).isEqualTo("false");
        assertThat(send("PUT", sakura.waiter(), "/api/v1/ratings/settings", "{\"waiterSeesAverage\":true}"))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(json(send("PUT", sakura.owner(), "/api/v1/ratings/settings", "{\"waiterSeesAverage\":true}"),
                "$.waiterSeesAverage")).isEqualTo("true");

        MvcTestResult mine = send("GET", sakura.waiter(), "/api/v1/ratings/mine", null);
        assertThat(json(mine, "$.visible")).isEqualTo("true");
        assertThat(json(mine, "$.summary.count")).isEqualTo("1");
        assertThat(json(mine, "$.summary.average")).isEqualTo("4.0");
        assertThat(send("GET", sakura.kitchen(), "/api/v1/ratings/settings", null)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void anotherRestaurantNeverSeesNorChangesIt() throws Exception {
        bill(sakura, ana);
        rate(sakura, ana, "{\"stars\":1,\"comment\":\"Segredo do Sakura\"}");
        String id = json(send("GET", sakura.owner(), "/api/v1/ratings/alerts", null), "$[0].id");
        Restaurant tanuki = newRestaurant("Tanuki");
        Cookie theirs = phone(tanuki);

        assertThat(send("POST", tanuki.owner(), "/api/v1/ratings/" + id + "/resolve", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", tanuki.owner(), "/api/v1/ratings", null).getResponse().getContentAsString())
                .doesNotContain("Segredo");
        assertThat(list(send("GET", tanuki.owner(), "/api/v1/ratings/alerts", null), "$[*]")).isEmpty();
        assertThat(json(send("GET", tanuki.owner(), "/api/v1/ratings/today", null), "$.count")).isEqualTo("0");
        // O cookie de um celular de outro restaurante não serve nesta mesa.
        assertThat(offer(sakura, theirs)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(json(send("GET", sakura.owner(), "/api/v1/ratings/alerts", null), "$[0].resolvedAt")).isEqualTo("null");
    }
}
