package br.com.cardapio.adapter.in.web.rodizio;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class RodizioControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String RODIZIOS = "/api/v1/rodizios";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

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

    /** Um produto no cardápio do restaurante. */
    private String product(String token, String name) throws Exception {
        MvcTestResult category = send("POST", token, "/api/v1/menu-categories",
                "{\"name\":\"Cat %s\"}".formatted(UUID.randomUUID().toString().substring(0, 8)));
        assertThat(category).hasStatus(HttpStatus.CREATED);
        MvcTestResult product = send("POST", token, "/api/v1/products", """
                {"categoryId":"%s","name":"%s","priceCents":2990,"featured":false,"available":true}
                """.formatted(json(category, "$.id"), name));
        assertThat(product).hasStatus(HttpStatus.CREATED);
        return json(product, "$.id");
    }

    /** O rodízio japonês: Adulto (com o jantar de sexta e sábado mais caro) e Criança. */
    private static String japones(String name, String... productIds) {
        String products = productIds.length == 0 ? "" : "\"" + String.join("\",\"", productIds) + "\"";
        return """
                {"name":"%s","type":"A_LA_MINUTE","description":"Peças à vontade",
                 "rules":{"maxPerItemPerRound":5,"timeLimitMinutes":120},
                 "tiers":[
                   {"name":"Adulto","defaultPriceCents":12990,"prices":[
                     {"days":["FRIDAY","SATURDAY"],"startsAt":"18:00","endsAt":"23:59","priceCents":14990}]},
                   {"name":"Criança","defaultPriceCents":6490,"prices":[]}],
                 "productIds":[%s]}
                """.formatted(name, products);
    }

    private String create(String token, String body) throws Exception {
        MvcTestResult created = send("POST", token, RODIZIOS, body);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
    }

    @Test
    void ownerCreatesEditsAndDeactivatesARodizio() throws Exception {
        String guioza = product(sakura.token(), "Guioza");
        String sushi = product(sakura.token(), "Sushi");

        MvcTestResult created = send("POST", sakura.token(), RODIZIOS, japones("Rodízio Japonês", guioza, sushi));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String id = json(created, "$.id");
        assertThat(created.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(RODIZIOS + "/" + id);

        MvcTestResult read = send("GET", sakura.token(), RODIZIOS + "/" + id, null);
        assertThat(read).hasStatusOk();
        assertThat(read).bodyJson().extractingPath("$.tiers[*].name").asArray().containsExactly("Adulto", "Criança");
        assertThat(read).bodyJson().extractingPath("$.tiers[0].prices[0].startsAt").isEqualTo("18:00:00");
        assertThat(read).bodyJson().extractingPath("$.tiers[0].prices[0].days").asArray()
                .containsExactlyInAnyOrder("FRIDAY", "SATURDAY");
        assertThat(read).bodyJson().extractingPath("$.rules.maxPerItemPerRound").isEqualTo(5);
        assertThat(read).bodyJson().extractingPath("$.rules.minRoundIntervalMinutes").isNull();
        assertThat(read).bodyJson().extractingPath("$.productIds").asArray().containsExactlyInAnyOrder(guioza, sushi);
        // No banco: sexta (16) + sábado (32), e o horário como o dono digitou, em qualquer fuso da JVM.
        assertThat(jdbc.queryForObject("SELECT days || ' ' || starts_at || '-' || ends_at FROM rodizio_price p "
                + "JOIN rodizio_tier t ON t.id = p.tier_id WHERE t.rodizio_id = ?", String.class, UUID.fromString(id)))
                .isEqualTo("48 18:00:00-23:59:00");

        String edited = """
                {"name":"Japonês Premium","type":"A_LA_MINUTE","rules":{},
                 "tiers":[{"name":"Adulto","defaultPriceCents":15990,"prices":[]}],
                 "productIds":["%s"]}
                """.formatted(sushi);
        MvcTestResult put = send("PUT", sakura.token(), RODIZIOS + "/" + id, edited);
        assertThat(put).hasStatusOk();
        assertThat(put).bodyJson().extractingPath("$.description").isEqualTo("");
        assertThat(put).bodyJson().extractingPath("$.tiers[*].name").asArray().containsExactly("Adulto");
        assertThat(put).bodyJson().extractingPath("$.productIds").asArray().containsExactly(sushi);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rodizio_price WHERE establishment_id = ?",
                Integer.class, sakura.establishmentId())).isZero();

        assertThat(send("DELETE", sakura.token(), RODIZIOS + "/" + id, null)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("GET", sakura.token(), RODIZIOS + "/" + id, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", sakura.token(), RODIZIOS, null)).bodyJson().extractingPath("$.totalElements")
                .isEqualTo(0);
    }

    @Test
    void theListIsPagedAndByName() throws Exception {
        create(sakura.token(), japones("Pizza"));
        create(sakura.token(), japones("Japonês"));
        create(sakura.token(), japones("Churrasco"));

        MvcTestResult page = send("GET", sakura.token(), RODIZIOS + "?page=0&size=2", null);
        assertThat(page).bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Churrasco", "Japonês");
        assertThat(page).bodyJson().extractingPath("$.totalElements").isEqualTo(3);
        assertThat(send("GET", sakura.token(), RODIZIOS + "?page=1&size=2", null))
                .bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Pizza");
        assertThat(send("GET", sakura.token(), RODIZIOS + "?size=101", null)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidDataGets400AndRepeatedNameGets409() throws Exception {
        create(sakura.token(), japones("Japonês"));

        MvcTestResult duplicate = send("POST", sakura.token(), RODIZIOS, japones("japonês"));
        assertThat(duplicate).hasStatus(HttpStatus.CONFLICT);

        MvcTestResult noTiers = send("POST", sakura.token(), RODIZIOS, """
                {"name":"Pizza","type":"A_LA_MINUTE","rules":{},"tiers":[],"productIds":[]}""");
        assertThat(noTiers).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(noTiers).bodyJson().extractingPath("$.fields").asArray().containsExactly("tiers");

        MvcTestResult badRule = send("POST", sakura.token(), RODIZIOS, """
                {"name":"Pizza","type":"A_LA_MINUTE","rules":{"timeLimitMinutes":5},
                 "tiers":[{"name":"Adulto","defaultPriceCents":-1,"prices":[]}],"productIds":[]}""");
        assertThat(badRule).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(badRule).bodyJson().extractingPath("$.fields").asArray()
                .containsExactlyInAnyOrder("rules.timeLimitMinutes", "tiers[0].defaultPriceCents");

        MvcTestResult overlapping = send("POST", sakura.token(), RODIZIOS, """
                {"name":"Pizza","type":"A_LA_MINUTE","rules":{},
                 "tiers":[{"name":"Adulto","defaultPriceCents":9990,"prices":[
                   {"days":["SATURDAY"],"startsAt":"18:00","endsAt":"23:00","priceCents":11990},
                   {"days":["SATURDAY","SUNDAY"],"startsAt":"22:00","endsAt":"23:30","priceCents":12990}]}],
                 "productIds":[]}""");
        assertThat(overlapping).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(overlapping, "$.detail")).contains("valem ao mesmo tempo");
    }

    @Test
    void aProductFromAnotherRestaurantIsRefused() throws Exception {
        Owner other = newRestaurant();
        String theirs = product(other.token(), "Guioza deles");

        MvcTestResult refused = send("POST", sakura.token(), RODIZIOS, japones("Japonês", theirs));

        assertThat(refused).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(refused, "$.detail")).contains("não está no cardápio");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rodizio WHERE establishment_id = ?", Integer.class,
                sakura.establishmentId())).isZero();
    }

    @Test
    void anotherRestaurantGets404() throws Exception {
        String id = create(sakura.token(), japones("Japonês"));
        Owner other = newRestaurant();

        assertThat(send("GET", other.token(), RODIZIOS + "/" + id, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PUT", other.token(), RODIZIOS + "/" + id, japones("Meu"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", other.token(), RODIZIOS + "/" + id, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.token(), RODIZIOS, null)).bodyJson().extractingPath("$.totalElements")
                .isEqualTo(0);
        assertThat(send("GET", sakura.token(), RODIZIOS + "/" + id, null)).bodyJson().extractingPath("$.name")
                .isEqualTo("Japonês");
    }

    @Test
    void theWaiterOnlyListsAndAnonymousCannotSee() throws Exception {
        String id = create(sakura.token(), japones("Japonês"));
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();

        assertThat(send("GET", waiter, RODIZIOS, null)).hasStatusOk();
        assertThat(send("GET", waiter, RODIZIOS + "/" + id, null)).hasStatusOk();
        assertThat(send("POST", waiter, RODIZIOS, japones("Pizza"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("PUT", waiter, RODIZIOS + "/" + id, japones("Pizza"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("DELETE", waiter, RODIZIOS + "/" + id, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", null, RODIZIOS, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
