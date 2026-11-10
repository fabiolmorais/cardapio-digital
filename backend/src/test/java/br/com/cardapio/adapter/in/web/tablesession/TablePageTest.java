package br.com.cardapio.adapter.in.web.tablesession;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As três rotas públicas da página da mesa, sem login: a mesa (com a marca), o cardápio e a
 * sessão. O dono monta o restaurante pela API autenticada; o "cliente" só tem o token do QR.
 */
@IntegrationTest
class TablePageTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    record Restaurant(String ownerToken, String qr, String tableId) {
    }

    private Restaurant sakura;

    @BeforeEach
    void sakuraWithATable() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
    }

    private Restaurant newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        String token = json(send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)), "$.accessToken");
        String sector = json(send("POST", token, "/api/v1/sectors", "{\"name\":\"Salão principal\"}"), "$.id");
        MvcTestResult table = send("POST", token, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector));
        return new Restaurant(token, json(table, "$.publicToken"), json(table, "$.id"));
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

    private String product(Restaurant r, String category, String name, long price, Long promo) throws Exception {
        MvcTestResult created = send("POST", r.ownerToken(), "/api/v1/products", """
                {"categoryId":"%s","name":"%s","priceCents":%d,"promoPriceCents":%s,"featured":%s,"available":true}
                """.formatted(category, name, price, promo, promo != null));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
    }

    @Test
    void theTableShowsTheBrandAndTheSector() throws Exception {
        send("PUT", sakura.ownerToken(), "/api/v1/establishment/brand", """
                {"name":"Sakura Sushi","color":"PETROL","theme":"DARK","instagram":"sakurasushi.sp"}""");

        MvcTestResult page = send("GET", null, "/api/v1/public/tables/" + sakura.qr(), null);

        assertThat(page).hasStatusOk();
        assertThat(json(page, "$.establishmentName")).isEqualTo("Sakura Sushi");
        assertThat(json(page, "$.tableLabel")).isEqualTo("04");
        assertThat(json(page, "$.sectorName")).isEqualTo("Salão principal");
        assertThat(json(page, "$.brand.theme")).isEqualTo("DARK");
        assertThat(json(page, "$.brand.primary")).isEqualTo("#0F766E");
        assertThat(json(page, "$.brand.accent")).isEqualTo("#5EEAD4"); // o tom do tema escuro
        assertThat(json(page, "$.brand.instagram")).isEqualTo("sakurasushi.sp");
        assertThat(json(page, "$.brand.logo")).isEqualTo("null");
    }

    @Test
    void theMenuHasTheCategoriesInOrderAndHidesWhatTheClientShouldNotSee() throws Exception {
        String entradas = json(send("POST", sakura.ownerToken(), "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        send("POST", sakura.ownerToken(), "/api/v1/menu-categories", "{\"name\":\"Vazia\"}");
        product(sakura, entradas, "Guioza", 2990, null);
        String sunomono = product(sakura, entradas, "Sunomono", 1990, 1590L);
        String antigo = product(sakura, entradas, "Prato antigo", 100, null);
        send("PATCH", sakura.ownerToken(), "/api/v1/products/" + sunomono + "/availability", "{\"available\":false}");
        mvc.delete().uri("/api/v1/products/" + antigo).header(HttpHeaders.AUTHORIZATION, "Bearer " + sakura.ownerToken())
                .exchange();

        MvcTestResult menu = send("GET", null, "/api/v1/public/tables/" + sakura.qr() + "/menu", null);

        assertThat(menu).hasStatusOk();
        assertThat(menu).bodyJson().extractingPath("$.categories[*].name").asArray().containsExactly("Entradas");
        assertThat(menu).bodyJson().extractingPath("$.categories[0].products[*].name").asArray()
                .containsExactly("Guioza", "Sunomono");
        assertThat(json(menu, "$.categories[0].products[1].available")).isEqualTo("false");
        assertThat(json(menu, "$.categories[0].products[1].promoPriceCents")).isEqualTo("1590");
        assertThat(json(menu, "$.categories[0].products[0].photo")).isEqualTo("null");
    }

    @Test
    void theMostOrderedComeFromTheLastThirtyDaysAndNeedAtLeastThree() throws Exception {
        String entradas = json(send("POST", sakura.ownerToken(), "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        String guioza = product(sakura, entradas, "Guioza", 2990, null);
        String sunomono = product(sakura, entradas, "Sunomono", 1990, null);
        String temaki = product(sakura, entradas, "Temaki", 3290, null);
        String edamame = product(sakura, entradas, "Edamame", 1990, null);

        // Poucos pratos pedidos ainda: a lista não diz nada.
        UUID order = order();
        item(order, guioza, 5, "DELIVERED", "now()");
        item(order, sunomono, 1, "DELIVERED", "now()");
        assertThat(send("GET", null, "/api/v1/public/tables/" + sakura.qr() + "/menu", null)).bodyJson()
                .extractingPath("$.mostOrdered").asArray().isEmpty();

        // Três pedidos no mês: entram, do mais pedido ao menos. O cancelado e o de 40 dias atrás não contam.
        item(order, temaki, 3, "PENDING", "now()");
        item(order, edamame, 20, "CANCELED", "now()");
        item(order, edamame, 20, "DELIVERED", "now() - interval '40 days'");
        assertThat(send("GET", null, "/api/v1/public/tables/" + sakura.qr() + "/menu", null)).bodyJson()
                .extractingPath("$.mostOrdered").asArray().containsExactly(guioza, temaki, sunomono);
    }

    /** Um pedido da mesa 04 numa comanda aberta, direto no banco (o fluxo do pedido está em OrderFlowTest). */
    private UUID order() {
        mvc.post().uri("/api/v1/public/tables/" + sakura.qr() + "/session").exchange(); // abre a sessão da mesa
        UUID table = UUID.fromString(sakura.tableId());
        UUID establishment = jdbc.queryForObject("SELECT establishment_id FROM dining_table WHERE id = ?", UUID.class, table);
        UUID session = jdbc.queryForObject("SELECT id FROM table_session WHERE table_id = ? LIMIT 1", UUID.class, table);
        UUID tab = UUID.randomUUID();
        jdbc.update("INSERT INTO tab (id, establishment_id, table_id, status, opened_at) VALUES (?, ?, ?, 'OPEN', now())",
                tab, establishment, table);
        UUID order = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customer_order (id, establishment_id, tab_id, table_id, session_id, number, status, created_at)
                VALUES (?, ?, ?, ?, ?, 1, 'SENT', now())""", order, establishment, tab, table, session);
        return order;
    }

    private void item(UUID order, String product, int quantity, String status, String createdAt) {
        jdbc.update("""
                INSERT INTO order_item (id, establishment_id, order_id, position, product_id, product_name,
                                        unit_price_cents, quantity, status, created_at)
                SELECT ?, o.establishment_id, o.id, 0, ?, 'x', 100, ?, ?, %s FROM customer_order o WHERE o.id = ?
                """.formatted(createdAt), UUID.randomUUID(), UUID.fromString(product), quantity, status, order);
    }

    @Test
    void theSessionLivesInAnHttpOnlyCookieAndIsResumed() throws Exception {
        String uri = "/api/v1/public/tables/" + sakura.qr() + "/session";

        MvcTestResult first = mvc.post().uri(uri).exchange();

        assertThat(first).hasStatusOk();
        Cookie cookie = first.getResponse().getCookie("table_session");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/public");
        assertThat(first.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Strict");
        assertThat(first.getResponse().getContentAsString()).doesNotContain(cookie.getValue());
        // O banco tem o hash, nunca o token do cookie.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM table_session WHERE token_hash = ?", Integer.class,
                cookie.getValue())).isZero();

        MvcTestResult again = mvc.post().uri(uri).cookie(cookie).exchange();
        assertThat(again.getResponse().getCookie("table_session").getValue()).isEqualTo(cookie.getValue());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM table_session WHERE table_id = ?::uuid", Integer.class,
                sakura.tableId())).isEqualTo(1);
    }

    @Test
    void anUnknownOrRotatedQrIsNotFoundEverywhere() throws Exception {
        String old = sakura.qr();
        send("POST", sakura.ownerToken(), "/api/v1/tables/" + sakura.tableId() + "/token-rotation", null);

        for (String uri : new String[]{"/api/v1/public/tables/" + old, "/api/v1/public/tables/" + old + "/menu",
                "/api/v1/public/tables/qr-que-ninguem-gerou-0000/menu"}) {
            assertThat(send("GET", null, uri, null)).as(uri).hasStatus(HttpStatus.NOT_FOUND);
        }
        assertThat(mvc.post().uri("/api/v1/public/tables/" + old + "/session").exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    /** Separação entre restaurantes: o QR de uma mesa só mostra o cardápio do restaurante dela. */
    @Test
    void eachQrShowsOnlyItsOwnRestaurant() throws Exception {
        Restaurant other = newRestaurant("Outro Bar");
        String theirs = json(send("POST", other.ownerToken(), "/api/v1/menu-categories", "{\"name\":\"Petiscos\"}"), "$.id");
        product(other, theirs, "Batata do Outro Bar", 2500, null);
        String mine = json(send("POST", sakura.ownerToken(), "/api/v1/menu-categories", "{\"name\":\"Entradas\"}"), "$.id");
        product(sakura, mine, "Guioza", 2990, null);

        String sakurasMenu = send("GET", null, "/api/v1/public/tables/" + sakura.qr() + "/menu", null)
                .getResponse().getContentAsString();

        assertThat(sakurasMenu).contains("Guioza").doesNotContain("Batata do Outro Bar", "Petiscos");
        assertThat(json(send("GET", null, "/api/v1/public/tables/" + other.qr(), null), "$.establishmentName"))
                .isEqualTo("Outro Bar");
    }
}
