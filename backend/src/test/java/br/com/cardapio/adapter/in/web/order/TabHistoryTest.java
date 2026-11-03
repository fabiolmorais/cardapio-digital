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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O histórico de comandas no admin: o período nos dias do restaurante (e não nos de UTC), o filtro
 * por mesa, os papéis e a separação entre restaurantes.
 */
@IntegrationTest
class TabHistoryTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** O dono, o garçom, as mesas 04 e 05 e um produto. */
    record Restaurant(String owner, String waiter, String qr04, String qr05, String table05, String guioza) {
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
        MvcTestResult table05 = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"05\",\"sectorId\":\"%s\"}".formatted(sector));
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        return new Restaurant(owner, staff(owner, establishment, "Carlos", Role.STAFF), qr04,
                json(table05, "$.publicToken"), json(table05, "$.id"), product(owner, category, "Guioza", 2990));
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

    /**
     * Uma visita inteira na mesa: um celular pede, o garçom entrega e fecha. Depois, o teste muda
     * a hora do fechamento direto no banco, para conferir os dias. Devolve o id da comanda.
     */
    private String visit(Restaurant r, String qr, int quantity, String closedAt) throws Exception {
        Cookie phone = mvc.post().uri("/api/v1/public/tables/" + qr + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + qr + "/presence").cookie(phone).exchange();
        MvcTestResult placed = mvc.post().uri("/api/v1/public/tables/" + qr + "/orders").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"%s\",\"quantity\":%d}]}".formatted(r.guioza(), quantity))
                .exchange();
        assertThat(placed).hasStatus(HttpStatus.CREATED);
        for (String item : list(placed, "$.items[*].id")) {
            send("POST", r.waiter(), "/api/v1/order-items/" + item + "/deliver", null);
        }
        String tabId = jdbc.queryForObject("SELECT tab_id FROM customer_order WHERE id = ?::uuid", String.class,
                json(placed, "$.id"));
        assertThat(send("POST", r.waiter(), "/api/v1/tabs/" + tabId + "/close", "{\"paymentMethod\":\"PIX\"}"))
                .hasStatusOk();
        jdbc.update("UPDATE tab SET closed_at = ?::timestamptz WHERE id = ?::uuid", closedAt, tabId);
        return tabId;
    }

    private MvcTestResult history(String token, String query) {
        return send("GET", token, "/api/v1/tabs?" + query, null);
    }

    @Test
    void theDaysAreTheRestaurantDays() throws Exception {
        // 23h30 do dia 3 em São Paulo, que já é dia 4 em UTC.
        String lateNight = visit(sakura, sakura.qr04(), 2, "2026-11-04T02:30:00Z");
        // Meio-dia do dia 4 em São Paulo.
        String noon = visit(sakura, sakura.qr05(), 1, "2026-11-04T15:00:00Z");

        MvcTestResult day3 = history(sakura.owner(), "from=2026-11-03&to=2026-11-03");
        assertThat(day3).hasStatusOk();
        assertThat(list(day3, "$.content[*].id")).containsExactly(lateNight);
        assertThat(json(day3, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(json(day3, "$.content[0].totalCents")).isEqualTo(String.valueOf(2 * 2990));
        assertThat(json(day3, "$.content[0].paymentMethod")).isEqualTo("PIX");
        assertThat(json(day3, "$.content[0].closedByName")).isEqualTo("Carlos");
        assertThat(list(history(sakura.owner(), "from=2026-11-04&to=2026-11-04"), "$.content[*].id"))
                .containsExactly(noon);
        // Os dois dias: a mais nova primeiro.
        assertThat(list(history(sakura.owner(), "from=2026-11-03&to=2026-11-04"), "$.content[*].id"))
                .containsExactly(noon, lateNight);
        assertThat(list(history(sakura.owner(), "from=2026-11-03&to=2026-11-04&tableId=" + sakura.table05()),
                "$.content[*].id")).containsExactly(noon);
        assertThat(list(history(sakura.owner(), "from=2026-11-05&to=2026-11-30"), "$.content")).isEmpty();
    }

    @Test
    void theOpenTabsAreNotHistory() throws Exception {
        Cookie phone = mvc.post().uri("/api/v1/public/tables/" + sakura.qr04() + "/session").exchange()
                .getResponse().getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + sakura.qr04() + "/presence").cookie(phone).exchange();
        mvc.post().uri("/api/v1/public/tables/" + sakura.qr04() + "/orders").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"%s\",\"quantity\":1}]}".formatted(sakura.guioza()))
                .exchange();
        String today = LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();

        assertThat(list(history(sakura.owner(), "from=" + today + "&to=" + today), "$.content")).isEmpty();
    }

    @Test
    void theWrongPeriodTheWrongRoleAndAnotherRestaurant() throws Exception {
        visit(sakura, sakura.qr04(), 1, "2026-11-03T15:00:00Z");
        Restaurant other = newRestaurant("Outro Bar");

        assertThat(history(sakura.owner(), "from=2026-11-04&to=2026-11-03")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(history(sakura.owner(), "from=2025-11-01&to=2026-11-03")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(history(sakura.owner(), "to=2026-11-03")).hasStatus(HttpStatus.BAD_REQUEST);
        // O histórico é da gestão: o garçom fecha a conta, mas não consulta o histórico.
        assertThat(history(sakura.waiter(), "from=2026-11-03&to=2026-11-03")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(list(history(other.owner(), "from=2026-11-03&to=2026-11-03"), "$.content")).isEmpty();
        assertThat(list(history(other.owner(), "from=2026-11-03&to=2026-11-03&tableId=" + sakura.table05()),
                "$.content")).isEmpty();
    }
}
