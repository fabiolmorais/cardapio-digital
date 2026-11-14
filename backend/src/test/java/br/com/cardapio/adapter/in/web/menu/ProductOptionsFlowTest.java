package br.com.cardapio.adapter.in.web.menu;

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
 * As opções dos produtos de ponta a ponta (ADR 0010): a biblioteca de grupos, os tamanhos e os grupos
 * de um produto, o cardápio da mesa, o pedido com o preço calculado no back e as escolhas copiadas na
 * comanda, na cozinha e na comanda impressa. E a regra crítica: outro restaurante recebe 404.
 */
@IntegrationTest
class ProductOptionsFlowTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String GROUPS = "/api/v1/option-groups";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    record Restaurant(String owner, UUID establishmentId, String qr, String category) {
    }

    /** A pizzaria: a pizza com tamanhos, os sabores meio a meio e a borda. */
    record Pizzeria(Restaurant r, String pizza, String broto, String grande, String flavors, String calabresa,
                    String frango, String crust, String catupiry) {
    }

    private Restaurant forno;

    @BeforeEach
    void pizzeria() throws Exception {
        forno = newRestaurant("Forno a Lenha");
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
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Pizzas\"}"), "$.id");
        return new Restaurant(owner, UUID.fromString(json(login, "$.establishment.id")), qr, category);
    }

    private String product(Restaurant r, String name, long price) throws Exception {
        return json(send("POST", r.owner(), "/api/v1/products", """
                {"categoryId":"%s","name":"%s","priceCents":%d,"featured":false,"available":true}
                """.formatted(r.category(), name, price)), "$.id");
    }

    private MvcTestResult createGroup(Restaurant r, String body) {
        return send("POST", r.owner(), GROUPS, body);
    }

    private Pizzeria pizzeria(Restaurant r) throws Exception {
        MvcTestResult flavors = createGroup(r, """
                {"name":"Sabores","title":"Escolha os sabores","minChoices":1,"maxChoices":2,"maxPerOption":1,
                 "splitsItem":true,"pricing":"HIGHEST","freeQuantity":0,
                 "items":[{"name":"Calabresa","priceCents":3990,"available":true},
                          {"name":"Frango com catupiry","priceCents":4490,"available":true}]}
                """);
        assertThat(flavors).hasStatus(HttpStatus.CREATED);
        MvcTestResult crust = createGroup(r, """
                {"title":"Borda","minChoices":0,"maxChoices":1,"maxPerOption":1,"splitsItem":false,
                 "pricing":"SUM","freeQuantity":0,"items":[{"name":"Catupiry","priceCents":800,"available":true}]}
                """);
        String pizza = product(r, "Pizza", 0);
        MvcTestResult options = send("PUT", r.owner(), "/api/v1/products/" + pizza + "/options", """
                {"variants":[{"name":"Broto","priceCents":0,"available":true,"rodizioDefault":false},
                             {"name":"Grande","priceCents":0,"available":true,"rodizioDefault":false}],
                 "groups":[{"groupId":"%s","limits":[{"variant":0,"minChoices":1,"maxChoices":1}]},
                           {"groupId":"%s","limits":[]}],
                 "prices":[{"optionId":"%s","variant":1,"priceCents":5490},
                           {"optionId":"%s","variant":1,"priceCents":5990}]}
                """.formatted(json(flavors, "$.id"), json(crust, "$.id"), json(flavors, "$.items[0].id"),
                json(flavors, "$.items[1].id")));
        assertThat(options).hasStatus(HttpStatus.OK);
        return new Pizzeria(r, pizza, json(options, "$.variants[0].id"), json(options, "$.variants[1].id"),
                json(flavors, "$.id"), json(flavors, "$.items[0].id"), json(flavors, "$.items[1].id"),
                json(crust, "$.id"), json(crust, "$.items[0].id"));
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
            case "PUT" -> mvc.put().uri(uri);
            case "PATCH" -> mvc.patch().uri(uri);
            case "DELETE" -> mvc.delete().uri(uri);
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

    private Cookie phone(String qr) {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + qr + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + qr + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult order(String qr, Cookie phone, String items) {
        return mvc.post().uri("/api/v1/public/tables/" + qr + "/orders").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[" + items + "]}").exchange();
    }

    private static String pizzaLine(Pizzeria p, String variant, int quantity, String... options) {
        String chosen = String.join(",", List.of(options).stream()
                .map(o -> "{\"optionId\":\"%s\",\"quantity\":1}".formatted(o)).toList());
        return "{\"productId\":\"%s\",\"quantity\":%d,\"variantId\":%s,\"options\":[%s]}".formatted(p.pizza(),
                quantity, variant == null ? "null" : "\"" + variant + "\"", chosen);
    }

    @Test
    void theOwnerBuildsTheLibraryAndThePizza() throws Exception {
        Pizzeria p = pizzeria(forno);

        MvcTestResult library = send("GET", forno.owner(), GROUPS, null);
        assertThat(library).bodyJson().extractingPath("$.content[*].name").asArray()
                .containsExactly("Borda", "Sabores");
        assertThat(library).bodyJson().extractingPath("$.content[1].productCount").isEqualTo(1);
        assertThat(library).bodyJson().extractingPath("$.content[1].splitsItem").isEqualTo(true);

        MvcTestResult options = send("GET", forno.owner(), "/api/v1/products/" + p.pizza() + "/options", null);
        assertThat(options).bodyJson().extractingPath("$.variants[*].name").asArray()
                .containsExactly("Broto", "Grande");
        // Sem nenhum marcado, o primeiro tamanho é o do rodízio.
        assertThat(options).bodyJson().extractingPath("$.variants[0].rodizioDefault").isEqualTo(true);
        assertThat(options).bodyJson().extractingPath("$.groups[0].group.title").isEqualTo("Escolha os sabores");
        assertThat(options).bodyJson().extractingPath("$.groups[0].limits[0].maxChoices").isEqualTo(1);
        assertThat(options).bodyJson().extractingPath("$.prices.length()").isEqualTo(2);
    }

    @Test
    void theTableSeesTheSizesTheGroupsAndTheFromPrice() throws Exception {
        Pizzeria p = pizzeria(forno);
        String guioza = product(forno, "Calzone", 4200);

        MvcTestResult menu = mvc.get().uri("/api/v1/public/tables/" + forno.qr() + "/menu").exchange();

        assertThat(menu).bodyJson().extractingPath("$.optionGroups[*].title").asArray()
                .containsExactly("Escolha os sabores", "Borda");
        String pizza = "$.categories[0].products[?(@.id == '" + p.pizza() + "')]";
        assertThat(menu).bodyJson().extractingPath(pizza + ".fromPriceCents").asArray().containsExactly(3990);
        assertThat(menu).bodyJson().extractingPath(pizza + ".variants[*].name").asArray()
                .containsExactly("Broto", "Grande");
        assertThat(menu).bodyJson().extractingPath(pizza + ".optionGroups[*].groupId").asArray()
                .containsExactly(p.flavors(), p.crust());
        // O produto de sempre não muda nada.
        String calzone = "$.categories[0].products[?(@.id == '" + guioza + "')]";
        assertThat(menu).bodyJson().extractingPath(calzone + ".fromPriceCents").asArray().containsOnlyNulls();
        assertThat(menu).bodyJson().extractingPath(calzone + ".variants[*]").asArray().isEmpty();
    }

    @Test
    void halfAndHalfIsChargedByTheMostExpensiveFlavorAndCopiedEverywhere() throws Exception {
        Pizzeria p = pizzeria(forno);
        Cookie ana = phone(forno.qr());

        MvcTestResult placed = order(forno.qr(), ana,
                pizzaLine(p, p.grande(), 2, p.calabresa(), p.frango(), p.catupiry()));

        assertThat(placed).hasStatus(HttpStatus.CREATED);
        // Grande: o sabor mais caro (59,90) + a borda (8,00).
        assertThat(placed).bodyJson().extractingPath("$.items[0].unitPriceCents").isEqualTo(5990 + 800);
        assertThat(placed).bodyJson().extractingPath("$.items[0].choices.variantName").isEqualTo("Grande");
        assertThat(placed).bodyJson().extractingPath("$.items[0].choices.options[*].name").asArray()
                .containsExactly("Calabresa", "Frango com catupiry", "Catupiry");
        assertThat(placed).bodyJson().extractingPath("$.items[0].choices.options[*].splitsItem").asArray()
                .containsExactly(true, true, false);

        MvcTestResult table = mvc.get().uri("/api/v1/public/tables/" + forno.qr() + "/orders").cookie(ana).exchange();
        assertThat(json(table, "$.totalCents")).isEqualTo(String.valueOf(2 * (5990 + 800)));

        MvcTestResult kitchen = send("GET", forno.owner(), "/api/v1/order-items", null);
        assertThat(kitchen).bodyJson().extractingPath("$.content[0].choices.options[*].name").asArray()
                .containsExactly("Calabresa", "Frango com catupiry", "Catupiry");

        MvcTestResult tickets = send("GET", forno.owner(), "/api/v1/print-jobs?kind=ORDER", null);
        assertThat(tickets).bodyJson().extractingPath("$.content[0].items[0].choices.variantName")
                .isEqualTo("Grande");

        // A cozinha só tinha para uma: as duas partes ficam com as mesmas escolhas.
        String item = json(placed, "$.items[0].id");
        assertThat(send("POST", forno.owner(), "/api/v1/order-items/" + item + "/cancel",
                "{\"reason\":\"Acabou a massa\",\"quantity\":1}")).hasStatus(HttpStatus.NO_CONTENT);
        String tabId = json(send("GET", forno.owner(), "/api/v1/tabs/open", null), "$.content[0].id");
        MvcTestResult tab = send("GET", forno.owner(), "/api/v1/tabs/" + tabId, null);
        assertThat(tab).bodyJson().extractingPath("$.orders[0].items[*].choices.options.length()").asArray()
                .containsExactly(3, 3);
        assertThat(json(tab, "$.totalCents")).isEqualTo(String.valueOf(5990 + 800));
    }

    @Test
    void theBackChecksEveryRule() throws Exception {
        Pizzeria p = pizzeria(forno);
        Cookie ana = phone(forno.qr());

        // Sem tamanho, sem sabor, broto com dois sabores: 400 com a regra.
        assertThat(order(forno.qr(), ana, pizzaLine(p, null, 1, p.calabresa()))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(order(forno.qr(), ana, pizzaLine(p, p.grande(), 1))).hasStatus(HttpStatus.BAD_REQUEST);
        MvcTestResult broto = order(forno.qr(), ana, pizzaLine(p, p.broto(), 1, p.calabresa(), p.frango()));
        assertThat(broto).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(broto, "$.detail")).isEqualTo("Pizza · Escolha os sabores: escolha 1.");

        // Esgotou o frango e o tamanho grande: 409, o cliente escolhe de novo.
        assertThat(send("PATCH", forno.owner(), "/api/v1/option-items/" + p.frango() + "/availability",
                "{\"available\":false}")).hasStatus(HttpStatus.OK);
        assertThat(order(forno.qr(), ana, pizzaLine(p, p.broto(), 1, p.frango()))).hasStatus(HttpStatus.CONFLICT);
        assertThat(send("PATCH", forno.owner(), "/api/v1/product-variants/" + p.grande() + "/availability",
                "{\"available\":false}")).hasStatus(HttpStatus.OK);
        assertThat(order(forno.qr(), ana, pizzaLine(p, p.grande(), 1, p.calabresa())))
                .hasStatus(HttpStatus.CONFLICT);

        // A opção de outro grupo (não ligado ao produto) também não passa.
        String other = json(createGroup(forno, """
                {"title":"Molhos","minChoices":0,"maxChoices":1,"maxPerOption":1,"splitsItem":false,
                 "pricing":"SUM","freeQuantity":0,"items":[{"name":"Barbecue","priceCents":0,"available":true}]}
                """), "$.items[0].id");
        assertThat(order(forno.qr(), ana, pizzaLine(p, p.broto(), 1, p.calabresa(), other)))
                .hasStatus(HttpStatus.CONFLICT);
        assertThat(order(forno.qr(), ana, pizzaLine(p, p.broto(), 1, p.calabresa()))).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void editingAGroupKeepsOldOrdersAndDeactivatingItTakesItOutOfTheProducts() throws Exception {
        Pizzeria p = pizzeria(forno);
        Cookie ana = phone(forno.qr());
        assertThat(order(forno.qr(), ana, pizzaLine(p, p.broto(), 1, p.calabresa()))).hasStatus(HttpStatus.CREATED);

        // Renomeia a calabresa e tira o frango (que fica desativado, não apagado).
        MvcTestResult edited = send("PUT", forno.owner(), GROUPS + "/" + p.flavors(), """
                {"name":"Sabores","title":"Escolha os sabores","minChoices":1,"maxChoices":2,"maxPerOption":1,
                 "splitsItem":true,"pricing":"HIGHEST","freeQuantity":0,
                 "items":[{"id":"%s","name":"Calabresa artesanal","priceCents":4190,"available":true}]}
                """.formatted(p.calabresa()));
        assertThat(edited).hasStatus(HttpStatus.OK);
        assertThat(edited).bodyJson().extractingPath("$.items[*].name").asArray().containsExactly("Calabresa artesanal");
        assertThat(jdbc.queryForObject("SELECT active FROM option_item WHERE id = ?", Boolean.class,
                UUID.fromString(p.frango()))).isFalse();

        MvcTestResult table = mvc.get().uri("/api/v1/public/tables/" + forno.qr() + "/orders").cookie(ana).exchange();
        assertThat(json(table, "$.orders[0].items[0].choices.options[0].name")).isEqualTo("Calabresa");

        assertThat(send("DELETE", forno.owner(), GROUPS + "/" + p.flavors(), null)).hasStatus(HttpStatus.NO_CONTENT);
        MvcTestResult options = send("GET", forno.owner(), "/api/v1/products/" + p.pizza() + "/options", null);
        assertThat(options).bodyJson().extractingPath("$.groups[*].group.title").asArray().containsExactly("Borda");
        assertThat(send("GET", forno.owner(), GROUPS + "/" + p.flavors(), null)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void removingASizeDeactivatesItAndTheProductCanGoBackToNormal() throws Exception {
        Pizzeria p = pizzeria(forno);

        MvcTestResult cleared = send("PUT", forno.owner(), "/api/v1/products/" + p.pizza() + "/options",
                "{\"variants\":[],\"groups\":[],\"prices\":[]}");

        assertThat(cleared).hasStatus(HttpStatus.OK);
        assertThat(cleared).bodyJson().extractingPath("$.variants").asArray().isEmpty();
        assertThat(jdbc.queryForList("SELECT active FROM product_variant WHERE product_id = ?", Boolean.class,
                UUID.fromString(p.pizza()))).containsOnly(false);
        // A pizza volta a ser um produto de sempre, pelo preço dela.
        assertThat(order(forno.qr(), phone(forno.qr()),
                "{\"productId\":\"%s\",\"quantity\":1}".formatted(p.pizza()))).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void theFormIsValidated() throws Exception {
        assertThat(createGroup(forno, """
                {"title":"Sabores","minChoices":1,"maxChoices":5,"maxPerOption":1,"splitsItem":true,
                 "pricing":"HIGHEST","freeQuantity":0,"items":[{"name":"A","priceCents":0,"available":true}]}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(createGroup(forno, """
                {"title":"Adicionais","minChoices":0,"maxChoices":2,"maxPerOption":1,"splitsItem":false,
                 "pricing":"SUM","freeQuantity":0,"items":[{"name":"Bacon","priceCents":0,"available":true},
                                                           {"name":"bacon","priceCents":0,"available":true}]}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        String pizza = product(forno, "Pizza", 0);
        assertThat(send("PUT", forno.owner(), "/api/v1/products/" + pizza + "/options", """
                {"variants":[{"name":"Grande","priceCents":0,"available":true,"rodizioDefault":true},
                             {"name":"Broto","priceCents":0,"available":true,"rodizioDefault":true}],
                 "groups":[],"prices":[]}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    /** A regra crítica do CLAUDE.md: outro restaurante não vê nem mexe em nada disto (404). */
    @Test
    void anotherRestaurantGetsNotFoundEverywhere() throws Exception {
        Pizzeria p = pizzeria(forno);
        Restaurant other = newRestaurant("Cantina Teste");
        String theirs = product(other, "Lasanha", 5000);
        String group = """
                {"title":"X","minChoices":0,"maxChoices":1,"maxPerOption":1,"splitsItem":false,
                 "pricing":"SUM","freeQuantity":0,"items":[{"name":"Y","priceCents":0,"available":true}]}
                """;

        assertThat(send("GET", other.owner(), GROUPS + "/" + p.flavors(), null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PUT", other.owner(), GROUPS + "/" + p.flavors(), group)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", other.owner(), GROUPS + "/" + p.flavors(), null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PATCH", other.owner(), "/api/v1/option-items/" + p.calabresa() + "/availability",
                "{\"available\":false}")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.owner(), "/api/v1/products/" + p.pizza() + "/options", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PUT", other.owner(), "/api/v1/products/" + p.pizza() + "/options",
                "{\"variants\":[],\"groups\":[],\"prices\":[]}")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PATCH", other.owner(), "/api/v1/product-variants/" + p.grande() + "/availability",
                "{\"available\":false}")).hasStatus(HttpStatus.NOT_FOUND);
        // Nem dá para ligar o grupo do outro restaurante a um produto seu.
        assertThat(send("PUT", other.owner(), "/api/v1/products/" + theirs + "/options",
                "{\"variants\":[],\"groups\":[{\"groupId\":\"%s\",\"limits\":[]}],\"prices\":[]}"
                        .formatted(p.flavors()))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.owner(), GROUPS, null)).bodyJson().extractingPath("$.content").asArray().isEmpty();
        // E nada mudou no restaurante dono.
        assertThat(send("GET", forno.owner(), GROUPS + "/" + p.flavors(), null)).hasStatus(HttpStatus.OK);
    }

    @Test
    void onlyOwnerAndManagerManageTheOptions() {
        assertThat(send("GET", null, GROUPS, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
