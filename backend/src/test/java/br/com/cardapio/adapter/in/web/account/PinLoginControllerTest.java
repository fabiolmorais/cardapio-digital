package br.com.cardapio.adapter.in.web.account;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class PinLoginControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String PIN = "2580";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    record Owner(String token, UUID establishmentId) {
    }

    private Owner sakura;
    private String carlos;
    private String phoneToken;

    @BeforeEach
    void restaurantWithAWaiterAndALinkedPhone() throws Exception {
        sakura = newRestaurant();
        carlos = addMember(sakura, "Carlos Souza", "STAFF");
        phoneToken = linkDevice(sakura, "WAITER");
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
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.establishment.id")));
    }

    private String addMember(Owner owner, String name, String role) throws Exception {
        MvcTestResult added = post(owner.token(), "/api/v1/team-members",
                "{\"name\":\"%s\",\"role\":\"%s\",\"pin\":\"%s\"}".formatted(name, role, PIN));
        assertThat(added).hasStatus(HttpStatus.CREATED);
        return json(added, "$.id");
    }

    /** O fluxo do Guia 5: abrir o pedido, o dono vincular e o aparelho buscar o token. */
    private String linkDevice(Owner owner, String type) throws Exception {
        MvcTestResult started = post(null, "/api/v1/device-pairings", "");
        String id = json(started, "$.id");
        String secret = json(started, "$.secret");
        assertThat(post(owner.token(), "/api/v1/devices", """
                {"code":"%s","name":"Aparelho %s","type":"%s"}""".formatted(json(started, "$.code"), type, type)))
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult claimed = post(null, "/api/v1/device-pairings/" + id + "/claim",
                "{\"secret\":\"%s\"}".formatted(secret));
        return claimed.getResponse().getCookie("device_token").getValue();
    }

    private MvcTestResult post(String token, String uri, String body) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        return (token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).exchange();
    }

    private MvcTestResult onDevice(String deviceToken, String uri) {
        var request = mvc.get().uri(uri);
        return (deviceToken == null ? request : request.cookie(new Cookie("device_token", deviceToken))).exchange();
    }

    private MvcTestResult pin(String deviceToken, String memberId, String pin) {
        var request = mvc.post().uri("/api/v1/auth/pin").contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"%s\",\"pin\":\"%s\"}".formatted(memberId, pin));
        return (deviceToken == null ? request : request.cookie(new Cookie("device_token", deviceToken))).exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    @Test
    void theDeviceShowsItsRestaurantAndWhoCanEnter() throws Exception {
        String rita = addMember(sakura, "Rita", "KITCHEN");
        String bia = addMember(sakura, "Bia", "STAFF");
        assertThat(mvc.delete().uri("/api/v1/team-members/" + bia)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sakura.token()).exchange())
                .hasStatus(HttpStatus.NO_CONTENT);

        MvcTestResult device = onDevice(phoneToken, "/api/v1/auth/device");
        assertThat(device).hasStatusOk();
        assertThat(device).bodyJson().extractingPath("$.device.name").isEqualTo("Aparelho WAITER");
        assertThat(device).bodyJson().extractingPath("$.establishmentName").asString().startsWith("Restaurante ");

        // Só quem entra por PIN: nem o dono (e-mail e senha) nem quem foi desligado.
        MvcTestResult members = onDevice(phoneToken, "/api/v1/auth/device/team-members");
        assertThat(members).hasStatusOk();
        assertThat(members).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(carlos, rita);
        assertThat(members.getResponse().getContentAsString()).doesNotContain("pin", PIN);
    }

    @Test
    void rightPinEntersAsTheWaiter() throws Exception {
        MvcTestResult login = pin(phoneToken, carlos, PIN);

        assertThat(login).hasStatusOk();
        assertThat(login).bodyJson().extractingPath("$.userName").isEqualTo("Carlos Souza");
        assertThat(login).bodyJson().extractingPath("$.establishment.role").isEqualTo("STAFF");
        assertThat(login).bodyJson().extractingPath("$.establishment.id").isEqualTo(sakura.establishmentId().toString());
        assertThat(login.getResponse().getHeader(HttpHeaders.SET_COOKIE)).startsWith("refresh_token=");

        MvcTestResult me = mvc.get().uri("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + json(login, "$.accessToken")).exchange();
        assertThat(me).hasStatusOk();
        assertThat(me).bodyJson().extractingPath("$.name").isEqualTo("Carlos Souza");
        assertThat(me).bodyJson().extractingPath("$.email").isNull();
    }

    @Test
    void wrongPinsCountDownLockAndTheLockExpires() throws Exception {
        for (int remaining = 4; remaining >= 1; remaining--) {
            MvcTestResult wrong = pin(phoneToken, carlos, "9999");
            assertThat(wrong).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(json(wrong, "$.detail")).contains(String.valueOf(remaining));
        }

        MvcTestResult locked = pin(phoneToken, carlos, "9999");
        assertThat(locked).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(locked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("900");
        assertThat(locked).bodyJson().extractingPath("$.minutesLeft").isEqualTo(15);
        assertThat(pin(phoneToken, carlos, PIN)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        // O tempo passa (direto no banco) e o PIN certo volta a entrar, zerando a contagem.
        jdbc.update("UPDATE pin_lockout SET locked_until = now() - interval '1 minute' WHERE membership_id = ?",
                UUID.fromString(carlos));
        assertThat(pin(phoneToken, carlos, PIN)).hasStatusOk();
        assertThat(jdbc.queryForObject("SELECT failed_attempts FROM pin_lockout WHERE membership_id = ?",
                Integer.class, UUID.fromString(carlos))).isZero();
    }

    /**
     * Oito tentativas erradas ao mesmo tempo para a mesma pessoa. Com a trava, elas entram uma
     * depois da outra: 4 erros contados, o quinto bloqueia, e as outras 3 já encontram o bloqueio.
     */
    @Test
    void parallelWrongPinsAreCountedExactly() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MvcTestResult>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 8; i++) {
                Callable<MvcTestResult> attempt = () -> {
                    start.await();
                    return pin(phoneToken, carlos, "9999");
                };
                futures.add(pool.submit(attempt));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<MvcTestResult> future : futures) {
                statuses.add(future.get().getResponse().getStatus());
            }

            assertThat(statuses).filteredOn(s -> s == HttpStatus.UNAUTHORIZED.value()).hasSize(4);
            assertThat(statuses).filteredOn(s -> s == HttpStatus.TOO_MANY_REQUESTS.value()).hasSize(4);
        }
        assertThat(jdbc.queryForObject("SELECT failed_attempts FROM pin_lockout WHERE membership_id = ?",
                Integer.class, UUID.fromString(carlos))).isEqualTo(5);
    }

    @Test
    void withoutALinkedDeviceNobodyEnters() {
        assertThat(pin(null, carlos, PIN)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(pin("inventado", carlos, PIN)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(onDevice(null, "/api/v1/auth/device")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void nobodyEntersWithPinOnTheTv() throws Exception {
        String tv = linkDevice(sakura, "TV");

        assertThat(onDevice(tv, "/api/v1/auth/device")).hasStatusOk();
        assertThat(onDevice(tv, "/api/v1/auth/device/team-members")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(pin(tv, carlos, PIN)).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Acesso entre restaurantes: o aparelho da Sakura não abre sessão para gente de outro restaurante. */
    @Test
    void memberOfAnotherRestaurantGets404OnThisDevice() throws Exception {
        Owner other = newRestaurant();
        String bruno = addMember(other, "Bruno", "STAFF");

        assertThat(pin(phoneToken, bruno, PIN)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(onDevice(phoneToken, "/api/v1/auth/device/team-members").getResponse().getContentAsString())
                .doesNotContain(bruno);
    }

    @Test
    void unlinkingTheDeviceEndsTheSessionsOpenedOnIt() throws Exception {
        MvcTestResult login = pin(phoneToken, carlos, PIN);
        String refreshToken = login.getResponse().getCookie("refresh_token").getValue();
        String deviceId = jdbc.queryForObject("SELECT id FROM device WHERE establishment_id = ?", UUID.class,
                sakura.establishmentId()).toString();

        assertThat(mvc.delete().uri("/api/v1/devices/" + deviceId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sakura.token()).exchange())
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", refreshToken)).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(onDevice(phoneToken, "/api/v1/auth/device")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
