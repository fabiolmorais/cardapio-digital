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
 * A taxa de serviço opcional, os 10% do garçom (RF39), de ponta a ponta: a configuração, a mesa que
 * tira, o garçom que confirma, o fechamento, o resumo do dia e o garçom que atendeu.
 */
@IntegrationTest
class ServiceChargeFlowTest {

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

    private static final String SETTINGS = """
            {"orderingEnabled":true,"approvalRequired":false,
             "serviceCharge":{"enabled":%s,"percent":%d,"destination":"%s"}}
            """;

    private void serviceCharge(Restaurant r, boolean enabled, int percent, String destination) {
        assertThat(send("PUT", r.owner(), "/api/v1/order-settings", SETTINGS.formatted(enabled, percent, destination)))
                .hasStatusOk();
    }

    /** O celular pede a conta (aceitando ou tirando a taxa) e o garçom aceita: devolve o id do chamado. */
    private String billAccepted(Restaurant r, Cookie phone, String method, boolean service) throws Exception {
        assertThat(mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/calls").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BILL\",\"paymentMethod\":\"%s\",\"serviceAccepted\":%s}".formatted(method, service))
                .exchange()).hasStatus(HttpStatus.CREATED);
        String callId = json(send("GET", r.waiter(), "/api/v1/calls", null), "$.content[0].id");
        assertThat(send("POST", r.waiter(), "/api/v1/calls/" + callId + "/accept", null)).hasStatusOk();
        return callId;
    }

    private MvcTestResult atTheTable(Restaurant r, Cookie phone) {
        return mvc.get().uri("/api/v1/public/tables/" + r.qr04() + "/orders").cookie(phone).exchange();
    }

    private String openTab(Restaurant r) throws Exception {
        return json(send("GET", r.waiter(), "/api/v1/tabs/open", null), "$.content[0].id");
    }

    @Test
    void itStartsOffAndNothingChangesInTheBill() throws Exception {
        Cookie ana = phone(sakura);
        order(sakura, ana, sakura.guioza(), 2);

        assertThat(json(send("GET", sakura.owner(), "/api/v1/order-settings", null), "$.serviceCharge.enabled"))
                .isEqualTo("false");
        MvcTestResult table = atTheTable(sakura, ana);
        assertThat(json(table, "$.dueCents")).isEqualTo("5980");
        assertThat(table.getResponse().getContentAsString()).doesNotContain("\"service\":{");
        assertThat(send("GET", sakura.waiter(), "/api/v1/tabs/" + openTab(sakura), null).getResponse()
                .getContentAsString()).doesNotContain("\"service\":{");
    }

    @Test
    void theTableSeesTheServiceAlreadyOnRemovesItAndTheWaiterConfirmsItAtTheMachine() throws Exception {
        serviceCharge(sakura, true, 10, "TEAM");
        Cookie ana = phone(sakura);
        MvcTestResult placed = order(sakura, ana, sakura.guioza(), 2);

        // Na tela da conta, os 10% já vêm marcados, com e sem calculados.
        MvcTestResult table = atTheTable(sakura, ana);
        assertThat(json(table, "$.totalCents")).isEqualTo("5980");
        assertThat(json(table, "$.service.percent")).isEqualTo("10");
        assertThat(json(table, "$.service.accepted")).isEqualTo("true");
        assertThat(json(table, "$.service.amountCents")).isEqualTo("598");
        assertThat(json(table, "$.service.dueWithCents")).isEqualTo("6578");
        assertThat(json(table, "$.service.dueWithoutCents")).isEqualTo("5980");
        assertThat(json(table, "$.service.destination")).isEqualTo("TEAM");
        assertThat(json(table, "$.dueCents")).isEqualTo("6578");

        // A mesa tira os 10% ao pedir a conta: o garçom vê sem.
        String callId = billAccepted(sakura, ana, "CARD", false);
        MvcTestResult bill = send("GET", sakura.waiter(), "/api/v1/calls/" + callId + "/bill", null);
        assertThat(json(bill, "$.service.accepted")).isEqualTo("false");
        assertThat(json(bill, "$.dueCents")).isEqualTo("5980");
        // Na maquininha, o cliente mudou de ideia: o garçom confirma a taxa e recebe tudo.
        assertThat(send("POST", sakura.waiter(), "/api/v1/calls/" + callId + "/payment",
                "{\"amountCents\":6578,\"paymentMethod\":\"CARD\",\"serviceAccepted\":true}")).hasStatusOk();

        deliver(sakura, placed);
        String tabId = openTab(sakura);
        MvcTestResult closed = send("POST", sakura.waiter(), "/api/v1/tabs/" + tabId + "/close", "{}");
        assertThat(closed).hasStatusOk();
        assertThat(json(closed, "$.totalCents")).isEqualTo("5980");
        assertThat(json(closed, "$.service.accepted")).isEqualTo("true");
        assertThat(json(closed, "$.service.amountCents")).isEqualTo("598");
        assertThat(jdbc.queryForObject("SELECT service_cents FROM tab WHERE id = ?::uuid", Long.class, tabId))
                .isEqualTo(598L);
        assertThat(jdbc.queryForObject("SELECT service_destination FROM tab WHERE id = ?::uuid", String.class, tabId))
                .isEqualTo("TEAM");

        // O resumo do dia: o faturado não leva a taxa; ela aparece separada.
        MvcTestResult floor = send("GET", sakura.owner(), "/api/v1/live/floor", null);
        assertThat(json(floor, "$.revenueTodayCents")).isEqualTo("5980");
        assertThat(json(floor, "$.serviceToday.totalCents")).isEqualTo("598");
        assertThat(list(floor, "$.serviceToday.byWaiter")).isEmpty();
    }

    @Test
    void withTheWaiterRuleTheServiceGoesToWhoServedTheTable() throws Exception {
        serviceCharge(sakura, true, 12, "WAITER");
        Cookie ana = phone(sakura);
        MvcTestResult placed = order(sakura, ana, sakura.sunomono(), 1);
        deliver(sakura, placed);

        assertThat(json(atTheTable(sakura, ana), "$.service.waiterName")).isEqualTo("Carlos");
        String tabId = openTab(sakura);
        MvcTestResult closed = send("POST", sakura.waiter(), "/api/v1/tabs/" + tabId + "/close",
                "{\"paymentMethod\":\"PIX\",\"serviceAccepted\":true}");

        assertThat(closed).hasStatusOk();
        // 12% de R$ 19,90 = R$ 2,388: arredonda para R$ 2,39.
        assertThat(json(closed, "$.service.amountCents")).isEqualTo("239");
        assertThat(json(closed, "$.service.waiterName")).isEqualTo("Carlos");
        assertThat(json(closed, "$.payments[0].amountCents")).isEqualTo(String.valueOf(1990 + 239));
        MvcTestResult floor = send("GET", sakura.owner(), "/api/v1/live/floor", null);
        assertThat(json(floor, "$.serviceToday.byWaiter[0].name")).isEqualTo("Carlos");
        assertThat(json(floor, "$.serviceToday.byWaiter[0].amountCents")).isEqualTo("239");
    }

    @Test
    void theWaiterCanRemoveTheServiceWhenClosing() throws Exception {
        serviceCharge(sakura, true, 10, "TEAM");
        Cookie ana = phone(sakura);
        deliver(sakura, order(sakura, ana, sakura.guioza(), 1));

        MvcTestResult closed = send("POST", sakura.waiter(), "/api/v1/tabs/" + openTab(sakura) + "/close",
                "{\"paymentMethod\":\"CASH\",\"serviceAccepted\":false}");

        assertThat(json(closed, "$.service.accepted")).isEqualTo("false");
        assertThat(json(closed, "$.payments[0].amountCents")).isEqualTo("2990");
        assertThat(json(send("GET", sakura.owner(), "/api/v1/live/floor", null), "$.serviceToday.totalCents"))
                .isEqualTo("0");
    }

    @Test
    void thePercentIsCopiedWhenTheBillIsRequested() throws Exception {
        serviceCharge(sakura, true, 10, "TEAM");
        Cookie ana = phone(sakura);
        order(sakura, ana, sakura.guioza(), 1);
        String callId = billAccepted(sakura, ana, "PIX", true);

        serviceCharge(sakura, true, 15, "TEAM");

        MvcTestResult bill = send("GET", sakura.waiter(), "/api/v1/calls/" + callId + "/bill", null);
        assertThat(json(bill, "$.service.percent")).isEqualTo("10");
        assertThat(json(bill, "$.dueCents")).isEqualTo(String.valueOf(2990 + 299));
    }

    @Test
    void onlyOwnerOrManagerSetsItAndOnlyFromOneToTwentyPercent() throws Exception {
        assertThat(send("PUT", sakura.owner(), "/api/v1/order-settings", SETTINGS.formatted(true, 25, "TEAM")))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("PUT", sakura.waiter(), "/api/v1/order-settings", SETTINGS.formatted(true, 10, "TEAM")))
                .hasStatus(HttpStatus.FORBIDDEN);
        // Outro restaurante ligando a taxa não muda nada aqui.
        Restaurant tanuki = newRestaurant("Tanuki");
        serviceCharge(tanuki, true, 10, "TEAM");
        assertThat(json(send("GET", sakura.owner(), "/api/v1/order-settings", null), "$.serviceCharge.enabled"))
                .isEqualTo("false");
    }
}
