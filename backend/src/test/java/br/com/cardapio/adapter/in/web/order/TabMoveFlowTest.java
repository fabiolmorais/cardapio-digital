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
 * Mudar a comanda de mesa e juntar comandas de ponta a ponta (RF44, ADR 0012): o que vai junto
 * (pedidos, chamados, celulares), a junção, a via impressa e o isolamento entre restaurantes.
 */
@IntegrationTest
class TabMoveFlowTest {

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

    record Table(String id, String qr) {
    }

    record Restaurant(String owner, UUID establishmentId, String joao, String kitchen, Table t04, Table t05,
                      Table t06, String chopp, String guioza) {
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
        assertThat(send("PUT", owner, "/api/v1/order-settings", SETTINGS_ON)).hasStatusOk();
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Bar\"}"), "$.id");
        return new Restaurant(owner, establishment, staff(owner, establishment, "João", Role.STAFF),
                staff(owner, establishment, "Rita", Role.KITCHEN), table(owner, sector, "04"),
                table(owner, sector, "05"), table(owner, sector, "06"),
                product(owner, category, "Chopp", 1200), product(owner, category, "Guioza", 2990));
    }

    private Table table(String owner, String sector, String label) throws Exception {
        MvcTestResult table = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"%s\",\"sectorId\":\"%s\"}".formatted(label, sector));
        return new Table(json(table, "$.id"), json(table, "$.publicToken"));
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

    /** O garçom lança um pedido na mesa; devolve o id da comanda. */
    private String order(Table table, String product, int quantity) throws Exception {
        MvcTestResult placed = send("POST", sakura.joao(), "/api/v1/tables/" + table.id() + "/orders", """
                {"clientRequestId":"%s","round":false,"confirmedDuplicates":["%s"],
                 "items":[{"productId":"%s","quantity":%d}]}
                """.formatted(UUID.randomUUID(), product, product, quantity));
        assertThat(placed).hasStatus(HttpStatus.CREATED);
        return json(placed, "$.tabId");
    }

    private MvcTestResult move(String token, String tabId, Table to) {
        return send("POST", token, "/api/v1/tabs/" + tabId + "/move", "{\"tableId\":\"%s\"}".formatted(to.id()));
    }

    private Cookie phone(Table table) {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + table.qr() + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + table.qr() + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult tab(String tabId) {
        return send("GET", sakura.owner(), "/api/v1/tabs/" + tabId, null);
    }

    @Test
    void theTabMovesToAFreeTableWithItsOrdersCallsAndPhones() throws Exception {
        Cookie ana = phone(sakura.t04());
        String tabId = order(sakura.t04(), sakura.chopp(), 2);
        assertThat(mvc.post().uri("/api/v1/public/tables/" + sakura.t04().qr() + "/calls").cookie(ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"WAITER\"}").exchange())
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult moved = move(sakura.joao(), tabId, sakura.t05());

        assertThat(moved).hasStatusOk();
        assertThat(json(moved, "$.tabId")).isEqualTo(tabId);
        assertThat(json(moved, "$.merged")).isEqualTo("false");
        MvcTestResult tab = tab(tabId);
        assertThat(json(tab, "$.tableLabel")).isEqualTo("05");
        assertThat(json(tab, "$.totalCents")).isEqualTo("2400");
        assertThat(json(tab, "$.moves[0].fromTableLabel")).isEqualTo("04");
        assertThat(json(tab, "$.moves[0].toTableLabel")).isEqualTo("05");
        assertThat(json(tab, "$.moves[0].movedByName")).isEqualTo("João");
        assertThat(list(send("GET", sakura.owner(), "/api/v1/tabs/open", null), "$.content[*].tableLabel"))
                .containsExactly("05");
        // A cozinha leva o prato para a mesa nova, e o chamado aberto foi junto.
        assertThat(list(send("GET", sakura.kitchen(), "/api/v1/order-items", null), "$.content[*].tableLabel"))
                .containsExactly("05");
        assertThat(jdbc.queryForObject("SELECT table_id::text FROM call WHERE establishment_id = ?",
                String.class, sakura.establishmentId())).isEqualTo(sakura.t05().id());

        // O celular da Ana continua: vê o pedido pela mesa nova, sem escanear de novo.
        MvcTestResult orders = mvc.get().uri("/api/v1/public/tables/" + sakura.t05().qr() + "/orders").cookie(ana)
                .exchange();
        assertThat(orders).hasStatusOk();
        assertThat(list(orders, "$.orders[*].items[*].productName")).containsExactly("Chopp");
        // Abrindo o QR antigo, a página é levada para a mesa nova.
        MvcTestResult reopened = mvc.post().uri("/api/v1/public/tables/" + sakura.t04().qr() + "/session")
                .cookie(ana).exchange();
        assertThat(json(reopened, "$.movedTo")).isEqualTo(sakura.t05().qr());
        // Quem chega depois na mesa 04 começa do zero.
        MvcTestResult stranger = mvc.post().uri("/api/v1/public/tables/" + sakura.t04().qr() + "/session")
                .exchange();
        assertThat(stranger.getResponse().getContentAsString()).doesNotContain("movedTo\":\"");
    }

    @Test
    void movingToATableWithAnOpenTabMergesTheTwo() throws Exception {
        String from = order(sakura.t04(), sakura.chopp(), 2);
        String into = order(sakura.t05(), sakura.guioza(), 1);

        MvcTestResult moved = move(sakura.owner(), from, sakura.t05());

        assertThat(moved).hasStatusOk();
        assertThat(json(moved, "$.merged")).isEqualTo("true");
        assertThat(json(moved, "$.tabId")).isEqualTo(into);
        MvcTestResult kept = tab(into);
        assertThat(json(kept, "$.totalCents")).isEqualTo(String.valueOf(2400 + 2990));
        assertThat(list(kept, "$.orders[*].items[*].productName")).containsExactlyInAnyOrder("Chopp", "Guioza");
        assertThat(json(kept, "$.moves[0].merged")).isEqualTo("true");
        MvcTestResult merged = tab(from);
        assertThat(json(merged, "$.status")).isEqualTo("MERGED");
        assertThat(json(merged, "$.mergedIntoTabId")).isEqualTo(into);
        assertThat(list(send("GET", sakura.owner(), "/api/v1/tabs/open", null), "$.content[*].tableLabel"))
                .containsExactly("05");
        // Juntada não aparece no histórico de fechadas, e não dá para mexer nela de novo.
        assertThat(move(sakura.owner(), from, sakura.t06())).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void bothTablesAskedForTheBillAndOnlyTheOlderCallStays() throws Exception {
        Cookie ana = phone(sakura.t04());
        Cookie bia = phone(sakura.t05());
        String from = order(sakura.t04(), sakura.chopp(), 1);
        order(sakura.t05(), sakura.guioza(), 1);
        String bill = "{\"type\":\"BILL\",\"paymentMethod\":\"PIX\"}";
        mvc.post().uri("/api/v1/public/tables/" + sakura.t04().qr() + "/calls").cookie(ana)
                .contentType(MediaType.APPLICATION_JSON).content(bill).exchange();
        mvc.post().uri("/api/v1/public/tables/" + sakura.t05().qr() + "/calls").cookie(bia)
                .contentType(MediaType.APPLICATION_JSON).content(bill).exchange();

        assertThat(move(sakura.joao(), from, sakura.t05())).hasStatusOk();

        assertThat(jdbc.queryForList("""
                SELECT table_id::text FROM call
                 WHERE establishment_id = ? AND status IN ('PENDING', 'IN_PROGRESS')
                """, String.class, sakura.establishmentId())).containsExactly(sakura.t05().id());
    }

    @Test
    void theKitchenGetsAMovedTicketForPrintedItemsOnTheWay() throws Exception {
        String tabId = order(sakura.t04(), sakura.chopp(), 2);
        String orderId = jdbc.queryForObject("SELECT id::text FROM customer_order WHERE tab_id = ?::uuid",
                String.class, tabId);
        assertThat(send("POST", sakura.kitchen(), "/api/v1/print-jobs/claim",
                "{\"orderId\":\"%s\",\"kind\":\"ORDER\"}".formatted(orderId))).hasStatusOk();

        move(sakura.joao(), tabId, sakura.t05());

        MvcTestResult pending = send("GET", sakura.kitchen(), "/api/v1/print-jobs/moves", null);
        assertThat(json(pending, "$.content[0].fromTableLabel")).isEqualTo("04");
        assertThat(json(pending, "$.content[0].toTableLabel")).isEqualTo("05");
        assertThat(list(pending, "$.content[0].items[*].productName")).containsExactly("Chopp");
        String moveId = json(pending, "$.content[0].moveId");
        MvcTestResult claimed = send("POST", sakura.kitchen(), "/api/v1/print-jobs/moves/" + moveId + "/claim", null);
        assertThat(list(claimed, "$.items[*].quantity")).containsExactly(2);
        // A segunda estação não imprime de novo, e a via sai da lista.
        assertThat(list(send("POST", sakura.kitchen(), "/api/v1/print-jobs/moves/" + moveId + "/claim", null),
                "$.items")).isEmpty();
        assertThat(list(send("GET", sakura.kitchen(), "/api/v1/print-jobs/moves", null), "$.content")).isEmpty();
    }

    @Test
    void withoutPrintedItemsThereIsNoTicket() throws Exception {
        String tabId = order(sakura.t04(), sakura.chopp(), 2);

        move(sakura.joao(), tabId, sakura.t05());

        assertThat(list(send("GET", sakura.kitchen(), "/api/v1/print-jobs/moves", null), "$.content")).isEmpty();
    }

    @Test
    void theSameTableOrADisabledOneIsRefused() throws Exception {
        String tabId = order(sakura.t04(), sakura.chopp(), 1);
        jdbc.update("UPDATE dining_table SET active = FALSE WHERE id = ?::uuid", sakura.t06().id());

        assertThat(move(sakura.joao(), tabId, sakura.t04())).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(move(sakura.joao(), tabId, sakura.t06())).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void theKitchenCannotMoveATab() throws Exception {
        String tabId = order(sakura.t04(), sakura.chopp(), 1);

        assertThat(move(sakura.kitchen(), tabId, sakura.t05())).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void anotherRestaurantGetsNotFound() throws Exception {
        String tabId = order(sakura.t04(), sakura.chopp(), 1);
        Restaurant tanuki = newRestaurant("Tanuki");

        // A comanda de outro restaurante, e a mesa de outro restaurante: 404 nos dois sentidos.
        assertThat(move(tanuki.owner(), tabId, tanuki.t05())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(move(sakura.owner(), tabId, tanuki.t05())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(json(tab(tabId), "$.tableLabel")).isEqualTo("04");
        assertThat(send("POST", tanuki.kitchen(), "/api/v1/print-jobs/moves/" + UUID.randomUUID() + "/claim", null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
