package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class MenuControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String CATEGORIES = "/api/v1/menu-categories";
    private static final String PRODUCTS = "/api/v1/products";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    record Owner(String token, UUID userId, UUID establishmentId) {
    }

    private Owner sakura;

    @BeforeEach
    void ownerOfSakura() throws Exception {
        sakura = newRestaurant();
    }

    private Owner newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String signup = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD);
        assertThat(send("POST", null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
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
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    private String category(String token, String name) throws Exception {
        MvcTestResult created = send("POST", token, CATEGORIES, "{\"name\":\"%s\"}".formatted(name));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
    }

    private static String productJson(String categoryId, String name, long priceCents, Long promoPriceCents) {
        return """
                {"categoryId":"%s","name":"%s","description":"","priceCents":%d,"promoPriceCents":%s,
                 "featured":false,"available":true}
                """.formatted(categoryId, name, priceCents, promoPriceCents);
    }

    private String product(String token, String categoryId, String name, long priceCents) throws Exception {
        MvcTestResult created = send("POST", token, PRODUCTS, productJson(categoryId, name, priceCents, null));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
    }

    private static String ids(String... ids) {
        return "{\"ids\":[\"" + String.join("\",\"", ids) + "\"]}";
    }

    @Test
    void ownerBuildsTheMenu() throws Exception {
        String entradas = category(sakura.token(), "Entradas");
        String sushis = category(sakura.token(), "Sushis");

        MvcTestResult hotRoll = send("POST", sakura.token(), PRODUCTS, """
                {"categoryId":"%s","name":"Hot roll da casa","description":"Empanado na hora. 4 peças.",
                 "priceCents":2790,"promoPriceCents":2490,"featured":true,"available":true}
                """.formatted(sushis));
        assertThat(hotRoll).hasStatus(HttpStatus.CREATED);
        assertThat(hotRoll.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo(PRODUCTS + "/" + json(hotRoll, "$.id"));
        product(sakura.token(), entradas, "Guioza (6 un.)", 2990);
        product(sakura.token(), entradas, "Sunomono", 1990);

        MvcTestResult categories = send("GET", sakura.token(), CATEGORIES, null);
        assertThat(categories).bodyJson().extractingPath("$.content[*].name").asArray()
                .containsExactly("Entradas", "Sushis");
        assertThat(categories).bodyJson().extractingPath("$.content[0].activeProducts").isEqualTo(2);

        assertThat(send("GET", sakura.token(), PRODUCTS, null)).bodyJson().extractingPath("$.content[*].name")
                .asArray().containsExactly("Guioza (6 un.)", "Sunomono", "Hot roll da casa");
        MvcTestResult one = send("GET", sakura.token(), PRODUCTS + "/" + json(hotRoll, "$.id"), null);
        assertThat(one).bodyJson().extractingPath("$.promoPriceCents").isEqualTo(2490);
        assertThat(one).bodyJson().extractingPath("$.featured").isEqualTo(true);
    }

    @Test
    void theFormReplacesEverythingAndAvailabilityHasItsOwnSwitch() throws Exception {
        String entradas = category(sakura.token(), "Entradas");
        String quentes = category(sakura.token(), "Pratos quentes");
        product(sakura.token(), quentes, "Yakisoba", 4990);
        String guioza = json(send("POST", sakura.token(), PRODUCTS, productJson(entradas, "Guioza", 2990, 2490L)),
                "$.id");

        MvcTestResult saved = send("PUT", sakura.token(), PRODUCTS + "/" + guioza, """
                {"categoryId":"%s","name":"Guioza grelhado","priceCents":3290,"featured":true,"available":true}
                """.formatted(quentes));
        assertThat(saved).hasStatusOk();
        assertThat(saved).bodyJson().extractingPath("$.categoryId").isEqualTo(quentes);
        assertThat(saved).bodyJson().extractingPath("$.position").isEqualTo(1);
        assertThat(saved).bodyJson().extractingPath("$.description").isEqualTo("");
        assertThat(saved).bodyJson().extractingPath("$.promoPriceCents").isNull();

        MvcTestResult soldOut = send("PATCH", sakura.token(), PRODUCTS + "/" + guioza + "/availability",
                "{\"available\":false}");
        assertThat(soldOut).bodyJson().extractingPath("$.available").isEqualTo(false);
        assertThat(send("GET", sakura.token(), PRODUCTS + "?categoryId=" + quentes, null))
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Yakisoba", "Guioza grelhado");
    }

    @Test
    void invalidDataGets400() throws Exception {
        String entradas = category(sakura.token(), "Entradas");

        MvcTestResult promoTooHigh = send("POST", sakura.token(), PRODUCTS, productJson(entradas, "Guioza", 2990, 2990L));
        assertThat(promoTooHigh).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(promoTooHigh, "$.detail")).isEqualTo("O preço promocional precisa ser menor que o preço normal");

        MvcTestResult negative = send("POST", sakura.token(), PRODUCTS, productJson(entradas, " ", -1, null));
        assertThat(negative).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(negative).bodyJson().extractingPath("$.fields").asArray().containsExactly("name", "priceCents");

        MvcTestResult noFlags = send("POST", sakura.token(), PRODUCTS,
                "{\"categoryId\":\"%s\",\"name\":\"Guioza\",\"priceCents\":2990}".formatted(entradas));
        assertThat(noFlags).bodyJson().extractingPath("$.fields").asArray().containsExactly("available", "featured");
    }

    /** .param() codifica o texto do jeito certo ("0%" vai como q=0%25). */
    private MvcTestResult search(String q) {
        return mvc.get().uri(PRODUCTS).param("q", q)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sakura.token()).exchange();
    }

    @Test
    void searchTreatsWildcardsAsLetters() throws Exception {
        String bebidas = category(sakura.token(), "Bebidas");
        product(sakura.token(), bebidas, "Suco 100% natural", 1200);
        product(sakura.token(), bebidas, "Suco de uva", 1000);
        product(sakura.token(), bebidas, "Água_com gás", 600);

        assertThat(search("SUCO")).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
        assertThat(search("0%")).bodyJson().extractingPath("$.content[*].name").asArray()
                .containsExactly("Suco 100% natural");
        assertThat(search("_")).bodyJson().extractingPath("$.content[*].name").asArray()
                .containsExactly("Água_com gás");
    }

    @Test
    void reorderingAndTheOutdatedScreen() throws Exception {
        String entradas = category(sakura.token(), "Entradas");
        String sushis = category(sakura.token(), "Sushis");
        String guioza = product(sakura.token(), entradas, "Guioza", 2990);
        String sunomono = product(sakura.token(), entradas, "Sunomono", 1990);

        assertThat(send("PUT", sakura.token(), CATEGORIES + "/order", ids(sushis, entradas)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("PUT", sakura.token(), CATEGORIES + "/" + entradas + "/product-order", ids(sunomono, guioza)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("GET", sakura.token(), CATEGORIES, null)).bodyJson().extractingPath("$.content[*].name")
                .asArray().containsExactly("Sushis", "Entradas");
        assertThat(send("GET", sakura.token(), PRODUCTS, null)).bodyJson().extractingPath("$.content[*].name")
                .asArray().containsExactly("Sunomono", "Guioza");

        // Outra aba criou um produto depois que a tela carregou: a ordem que a tela manda está velha.
        product(sakura.token(), entradas, "Edamame", 1990);
        MvcTestResult outdated = send("PUT", sakura.token(), CATEGORIES + "/" + entradas + "/product-order",
                ids(guioza, sunomono));
        assertThat(outdated).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(outdated, "$.detail")).startsWith("A lista mudou");

        assertThat(send("PUT", sakura.token(), CATEGORIES + "/order", ids(sushis, sushis, entradas)))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void categoryMustBeEmptyToLeaveAndRepeatedNamesGet409() throws Exception {
        String entradas = category(sakura.token(), "Entradas");
        String guioza = product(sakura.token(), entradas, "Guioza", 2990);

        assertThat(send("POST", sakura.token(), CATEGORIES, "{\"name\":\"ENTRADAS\"}")).hasStatus(HttpStatus.CONFLICT);

        MvcTestResult notEmpty = send("DELETE", sakura.token(), CATEGORIES + "/" + entradas, null);
        assertThat(notEmpty).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(notEmpty, "$.detail")).isEqualTo("Esta categoria tem 1 produto. Mova ou desative o produto antes.");

        assertThat(send("DELETE", sakura.token(), PRODUCTS + "/" + guioza, null)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("GET", sakura.token(), PRODUCTS + "/" + guioza, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", sakura.token(), CATEGORIES + "/" + entradas, null)).hasStatus(HttpStatus.NO_CONTENT);
        // Desativada, a categoria libera o nome.
        category(sakura.token(), "Entradas");
    }

    @Test
    void waiterAndKitchenCannotEditTheMenuAndAnonymousCannotSeeIt() {
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            String token = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                    new EstablishmentId(sakura.establishmentId()), role)).value();

            assertThat(send("GET", token, PRODUCTS, null)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(send("POST", token, CATEGORIES, "{\"name\":\"Bebidas\"}")).hasStatus(HttpStatus.FORBIDDEN);
        }
        assertThat(send("GET", null, CATEGORIES, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Acesso entre restaurantes: o dono de outro restaurante não vê nem mexe em nada do cardápio da Sakura. */
    @Test
    void ownerOfAnotherRestaurantGets404AndChangesNothing() throws Exception {
        String entradas = category(sakura.token(), "Entradas");
        String sushis = category(sakura.token(), "Sushis");
        String guioza = product(sakura.token(), entradas, "Guioza", 2990);
        Owner other = newRestaurant();
        String theirs = category(other.token(), "Deles");

        assertThat(send("PATCH", other.token(), CATEGORIES + "/" + entradas, "{\"name\":\"Minhas\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", other.token(), CATEGORIES + "/" + sushis, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PUT", other.token(), CATEGORIES + "/" + entradas + "/product-order", ids(guioza)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", other.token(), PRODUCTS, productJson(entradas, "Intruso", 100, null)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.token(), PRODUCTS + "/" + guioza, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PUT", other.token(), PRODUCTS + "/" + guioza, productJson(theirs, "Meu", 100, null)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PATCH", other.token(), PRODUCTS + "/" + guioza + "/availability", "{\"available\":false}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", other.token(), PRODUCTS + "/" + guioza, null)).hasStatus(HttpStatus.NOT_FOUND);
        // Os ids da Sakura não fazem parte da lista dele: a ordem "está velha" e nada muda.
        assertThat(send("PUT", other.token(), CATEGORIES + "/order", ids(sushis, entradas, theirs)))
                .hasStatus(HttpStatus.CONFLICT);
        assertThat(send("GET", other.token(), PRODUCTS + "?categoryId=" + entradas, null))
                .bodyJson().extractingPath("$.totalElements").isEqualTo(0);

        // E o meu produto continua igual.
        String mine = send("GET", sakura.token(), PRODUCTS + "/" + guioza, null).getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(mine, "$.name")).isEqualTo("Guioza");
        assertThat(JsonPath.<Boolean>read(mine, "$.available")).isTrue();
        assertThat(send("GET", sakura.token(), CATEGORIES, null)).bodyJson().extractingPath("$.content[*].name")
                .asArray().containsExactly("Entradas", "Sushis");
    }
}
