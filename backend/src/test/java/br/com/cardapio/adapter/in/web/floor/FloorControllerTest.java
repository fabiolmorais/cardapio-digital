package br.com.cardapio.adapter.in.web.floor;

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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class FloorControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String SECTORS = "/api/v1/sectors";
    private static final String TABLES = "/api/v1/tables";

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

    private String createSector(String token, String name) throws Exception {
        MvcTestResult created = send("POST", token, SECTORS, "{\"name\":\"%s\"}".formatted(name));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
    }

    private MvcTestResult createTable(String token, String sectorId, String label) {
        return send("POST", token, TABLES, "{\"label\":\"%s\",\"sectorId\":\"%s\"}".formatted(label, sectorId));
    }

    private String tableId(String token, String sectorId, String label) throws Exception {
        MvcTestResult created = createTable(token, sectorId, label);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
    }

    @Test
    void ownerSetsUpTheFloor() throws Exception {
        String salao = createSector(sakura.token(), "Salão principal");
        String deck = createSector(sakura.token(), "Deck");

        MvcTestResult t04 = createTable(sakura.token(), salao, "04");
        assertThat(t04).hasStatus(HttpStatus.CREATED);
        assertThat(t04.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(TABLES + "/" + json(t04, "$.id"));
        assertThat(json(t04, "$.publicToken")).matches("[A-Za-z0-9_-]{43}");
        tableId(sakura.token(), salao, "10");
        tableId(sakura.token(), salao, "2");
        tableId(sakura.token(), deck, "D1");

        MvcTestResult sectors = send("GET", sakura.token(), SECTORS, null);
        assertThat(sectors).bodyJson().extractingPath("$.content[*].name").asArray()
                .containsExactly("Salão principal", "Deck");
        assertThat(sectors).bodyJson().extractingPath("$.content[0].activeTables").isEqualTo(3);

        assertThat(send("GET", sakura.token(), TABLES + "?sectorId=" + salao, null))
                .bodyJson().extractingPath("$.content[*].label").asArray().containsExactly("2", "04", "10");

        assertThat(send("PATCH", sakura.token(), SECTORS + "/" + deck, "{\"name\":\"Varanda\"}"))
                .bodyJson().extractingPath("$.name").isEqualTo("Varanda");
    }

    @Test
    void repeatedNamesGet409() throws Exception {
        String salao = createSector(sakura.token(), "Salão");
        tableId(sakura.token(), salao, "04");

        MvcTestResult sameSector = send("POST", sakura.token(), SECTORS, "{\"name\":\"salão\"}");
        assertThat(sameSector).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(sameSector, "$.detail")).isEqualTo("Já existe um setor chamado \"salão\"");
        assertThat(createTable(sakura.token(), salao, " 04 ")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void invalidDataGets400() throws Exception {
        String salao = createSector(sakura.token(), "Salão");

        MvcTestResult blank = send("POST", sakura.token(), SECTORS, "{\"name\":\" \"}");
        assertThat(blank).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(blank).bodyJson().extractingPath("$.fields").asArray().containsExactly("name");

        MvcTestResult strange = createTable(sakura.token(), salao, "Mesa/4");
        assertThat(strange).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(strange, "$.detail")).contains("letras, números");
    }

    @Test
    void movingDeactivatingAndTheSectorThatMustBeEmpty() throws Exception {
        String salao = createSector(sakura.token(), "Salão");
        String deck = createSector(sakura.token(), "Deck");
        String table = tableId(sakura.token(), deck, "20");

        MvcTestResult notEmpty = send("DELETE", sakura.token(), SECTORS + "/" + deck, null);
        assertThat(notEmpty).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(notEmpty, "$.detail")).isEqualTo("Este setor tem 1 mesa ativa. Mova ou desative a mesa antes.");

        MvcTestResult moved = send("PATCH", sakura.token(), TABLES + "/" + table, "{\"sectorId\":\"%s\"}".formatted(salao));
        assertThat(moved).bodyJson().extractingPath("$.sectorId").isEqualTo(salao);
        assertThat(send("DELETE", sakura.token(), SECTORS + "/" + deck, null)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("GET", sakura.token(), SECTORS, null)).bodyJson().extractingPath("$.content[*].name")
                .asArray().containsExactly("Salão");

        MvcTestResult off = send("PATCH", sakura.token(), TABLES + "/" + table, "{\"active\":false}");
        assertThat(off).bodyJson().extractingPath("$.active").isEqualTo(false);
        // Desativada, ainda bloqueia o rótulo: a ideia é reativá-la.
        assertThat(createTable(sakura.token(), salao, "20")).hasStatus(HttpStatus.CONFLICT);
        assertThat(send("PATCH", sakura.token(), TABLES + "/" + table, "{\"active\":true}"))
                .bodyJson().extractingPath("$.active").isEqualTo(true);
    }

    @Test
    void rotatingTheTokenReplacesTheOldOne() throws Exception {
        String salao = createSector(sakura.token(), "Salão");
        MvcTestResult created = createTable(sakura.token(), salao, "04");
        String oldToken = json(created, "$.publicToken");

        MvcTestResult rotated = send("POST", sakura.token(), TABLES + "/" + json(created, "$.id") + "/token-rotation", null);

        assertThat(rotated).hasStatusOk();
        assertThat(json(rotated, "$.publicToken")).isNotEqualTo(oldToken);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dining_table WHERE public_token = ?", Integer.class, oldToken))
                .isZero();
    }

    @Test
    void waiterCannotManageTheFloorAndAnonymousCannotSeeIt() {
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();

        assertThat(send("GET", waiter, TABLES, null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("POST", waiter, SECTORS, "{\"name\":\"Deck\"}")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", null, SECTORS, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Acesso entre restaurantes: o dono de outro restaurante não vê nem mexe em nada do salão da Sakura. */
    @Test
    void ownerOfAnotherRestaurantGets404AndChangesNothing() throws Exception {
        String salao = createSector(sakura.token(), "Salão");
        MvcTestResult created = createTable(sakura.token(), salao, "04");
        String table = json(created, "$.id");
        Owner other = newRestaurant();

        assertThat(send("PATCH", other.token(), TABLES + "/" + table, "{\"label\":\"99\"}")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", other.token(), TABLES + "/" + table + "/token-rotation", null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(createTable(other.token(), salao, "77")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PATCH", other.token(), SECTORS + "/" + salao, "{\"name\":\"Meu\"}")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("DELETE", other.token(), SECTORS + "/" + salao, null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.token(), TABLES, null)).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
        assertThat(send("GET", other.token(), TABLES + "?sectorId=" + salao, null))
                .bodyJson().extractingPath("$.totalElements").isEqualTo(0);

        MvcTestResult mine = send("GET", sakura.token(), TABLES, null);
        assertThat(mine).bodyJson().extractingPath("$.content[0].label").isEqualTo("04");
        assertThat(mine).bodyJson().extractingPath("$.content[0].publicToken").isEqualTo(json(created, "$.publicToken"));
    }

    /**
     * Dois cadastros da mesma mesa ao mesmo tempo: os dois passam pela checagem do caso de uso,
     * mas o índice único barra o segundo, e o erro do banco vira 409 (e não 500).
     */
    @Test
    void twoTablesWithTheSameLabelAtTheSameTimeGiveOne201AndOne409() throws Exception {
        String salao = createSector(sakura.token(), "Salão");
        for (int round = 0; round < 5; round++) {
            String label = "R" + round;
            CountDownLatch start = new CountDownLatch(1);
            Callable<MvcTestResult> create = () -> {
                start.await();
                return createTable(sakura.token(), salao, label);
            };
            List<MvcTestResult> results;
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                Future<MvcTestResult> first = pool.submit(create);
                Future<MvcTestResult> second = pool.submit(create);
                start.countDown();
                results = List.of(first.get(), second.get());
            }
            assertThat(results).extracting(r -> r.getResponse().getStatus()).as("rodada %d", round)
                    .containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
        }
    }
}
