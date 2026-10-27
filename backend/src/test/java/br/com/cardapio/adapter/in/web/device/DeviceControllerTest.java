package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.device.model.DeviceSecretHash;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class DeviceControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String DEVICES = "/api/v1/devices";
    private static final String PAIRINGS = "/api/v1/device-pairings";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    record Owner(String token, UUID userId, UUID establishmentId) {
    }

    /** O que o aparelho novo recebe ao abrir o pedido. */
    record Pairing(String id, String code, String secret) {
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
        assertThat(post(null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = post(null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
    }

    private MvcTestResult post(String token, String uri, String body) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        return (token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).exchange();
    }

    private MvcTestResult get(String token, String uri) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String token, String id) {
        return mvc.delete().uri(DEVICES + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    /** O aparelho novo abre o pedido (sem login nenhum). */
    private Pairing startPairing() throws Exception {
        MvcTestResult started = post(null, PAIRINGS, "");
        assertThat(started).hasStatus(HttpStatus.CREATED);
        return new Pairing(json(started, "$.id"), json(started, "$.code"), json(started, "$.secret"));
    }

    private MvcTestResult claim(Pairing pairing) {
        return claim(pairing.id(), pairing.secret());
    }

    private MvcTestResult claim(String id, String secret) {
        return post(null, PAIRINGS + "/" + id + "/claim", "{\"secret\":\"%s\"}".formatted(secret));
    }

    private MvcTestResult link(String token, String code) {
        return post(token, DEVICES, """
                {"code":"%s","name":"Celular do salão","type":"WAITER"}""".formatted(code));
    }

    @Test
    void newDeviceShowsACodeTheManagerTypesAndThenTheDeviceGetsItsToken() throws Exception {
        Pairing pairing = startPairing();
        assertThat(pairing.code()).matches("\\d{6}");

        MvcTestResult pending = claim(pairing);
        assertThat(pending).hasStatusOk();
        assertThat(pending).bodyJson().extractingPath("$.status").isEqualTo("PENDING");
        assertThat(pending.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();

        MvcTestResult linked = link(sakura.token(), pairing.code());
        assertThat(linked).hasStatus(HttpStatus.CREATED);
        String deviceId = json(linked, "$.id");
        assertThat(linked.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(DEVICES + "/" + deviceId);
        assertThat(linked).bodyJson().extractingPath("$.lastSeenAt").isNull();

        MvcTestResult delivered = claim(pairing);
        assertThat(delivered).hasStatusOk();
        assertThat(delivered).bodyJson().extractingPath("$.status").isEqualTo("LINKED");
        assertThat(delivered).bodyJson().extractingPath("$.device.id").isEqualTo(deviceId);
        assertThat(delivered).bodyJson().extractingPath("$.establishmentName").asString().startsWith("Restaurante ");
        assertThat(delivered.getResponse().getHeader(HttpHeaders.SET_COOKIE)).startsWith("device_token=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=");

        // O token só vai no cookie, nunca no corpo; o banco guarda só o hash.
        Cookie cookie = delivered.getResponse().getCookie(DeviceTokenCookie.NAME);
        assertThat(cookie).isNotNull();
        assertThat(delivered.getResponse().getContentAsString()).doesNotContain(cookie.getValue());
        String storedHash = jdbc.queryForObject("SELECT token_hash FROM device WHERE id = ?", String.class,
                UUID.fromString(deviceId));
        assertThat(storedHash).isEqualTo(DeviceSecretHash.of(cookie.getValue()).value());

        assertThat(claim(pairing)).hasStatus(HttpStatus.NOT_FOUND); // só uma vez

        MvcTestResult list = get(sakura.token(), DEVICES);
        assertThat(list).bodyJson().extractingPath("$.content[0].name").isEqualTo("Celular do salão");
        assertThat(list).bodyJson().extractingPath("$.content[0].lastSeenAt").isNotNull();
    }

    @Test
    void wrongSecretOrUnknownPairingGets404() throws Exception {
        Pairing pairing = startPairing();

        assertThat(claim(pairing.id(), "chutado")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(claim(UUID.randomUUID().toString(), pairing.secret())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(link(sakura.token(), "000000")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void expiredCodeCannotBeLinked() throws Exception {
        Pairing pairing = startPairing();
        jdbc.update("""
                UPDATE device_pairing SET created_at = now() - interval '20 minutes',
                                          expires_at = now() - interval '10 minutes' WHERE id = ?""",
                UUID.fromString(pairing.id()));

        MvcTestResult tooLate = link(sakura.token(), pairing.code());
        assertThat(tooLate).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(json(tooLate, "$.detail")).isEqualTo("Código não encontrado ou vencido. Gere um novo no aparelho.");
        assertThat(claim(pairing)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidDataGets400() throws Exception {
        MvcTestResult invalid = post(sakura.token(), DEVICES, "{\"code\":\"12ab56\",\"name\":\" \",\"type\":\"WAITER\"}");

        assertThat(invalid).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(invalid).bodyJson().extractingPath("$.fields").asArray().containsExactly("code", "name");
    }

    @Test
    void waiterCannotManageDevicesAndAnonymousCannotSeeThem() {
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();

        assertThat(get(waiter, DEVICES)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri(DEVICES).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Acesso entre restaurantes: o dono de outro restaurante não vê nem desvincula. */
    @Test
    void ownerOfAnotherRestaurantGets404AndUnlinkingWorksForTheRightOwner() throws Exception {
        Pairing pairing = startPairing();
        String deviceId = json(link(sakura.token(), pairing.code()), "$.id");
        Owner other = newRestaurant();

        assertThat(delete(other.token(), deviceId)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(other.token(), DEVICES)).bodyJson().extractingPath("$.totalElements").isEqualTo(0);

        assertThat(delete(sakura.token(), deviceId)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(delete(sakura.token(), deviceId)).hasStatus(HttpStatus.NO_CONTENT); // de novo: tudo bem
        assertThat(get(sakura.token(), DEVICES)).bodyJson().extractingPath("$.totalElements").isEqualTo(0);
        // Desvinculado antes de buscar o token: o aparelho não recebe mais nada.
        assertThat(claim(pairing)).hasStatus(HttpStatus.NOT_FOUND);
    }

    /**
     * Dois gerentes (aqui, de restaurantes diferentes) digitam o mesmo código ao mesmo tempo:
     * só um vincula, e só um aparelho é criado.
     */
    @Test
    void twoManagersTypingTheSameCodeAtTheSameTimeLinkOnlyOneDevice() throws Exception {
        Owner other = newRestaurant();
        for (int round = 0; round < 5; round++) {
            Pairing pairing = startPairing();
            CountDownLatch start = new CountDownLatch(1);
            Callable<MvcTestResult> sakuraLinks = () -> {
                start.await();
                return link(sakura.token(), pairing.code());
            };
            Callable<MvcTestResult> otherLinks = () -> {
                start.await();
                return link(other.token(), pairing.code());
            };

            List<MvcTestResult> results;
            try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
                Future<MvcTestResult> first = pool.submit(sakuraLinks);
                Future<MvcTestResult> second = pool.submit(otherLinks);
                start.countDown();
                results = List.of(first.get(), second.get());
            }

            assertThat(results).extracting(r -> r.getResponse().getStatus())
                    .as("rodada %d", round)
                    .containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.NOT_FOUND.value());
            Integer devicesForThisPairing = jdbc.queryForObject("""
                    SELECT count(*) FROM device d JOIN device_pairing p ON p.device_id = d.id WHERE p.id = ?""",
                    Integer.class, UUID.fromString(pairing.id()));
            assertThat(devicesForThisPairing).isEqualTo(1);
        }
    }
}
