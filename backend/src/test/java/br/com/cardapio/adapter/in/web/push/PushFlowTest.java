package br.com.cardapio.adapter.in.web.push;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.FakePushService;
import br.com.cardapio.support.IntegrationTest;
import br.com.cardapio.support.TestVapidKeys;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As notificações de ponta a ponta: a equipe se inscreve, a mesa chama, e o serviço de push (falso,
 * na própria máquina) recebe uma notificação para cada pessoa ativa do salão, depois do commit.
 * As chaves VAPID são geradas na hora, só para este teste.
 */
@IntegrationTest
class PushFlowTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final TestVapidKeys VAPID = TestVapidKeys.generate();
    private static final FakePushService PUSH = start();

    private static FakePushService start() {
        try {
            return new FakePushService();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void pushOn(DynamicPropertyRegistry registry) {
        registry.add("app.push.vapid-public-key", VAPID::publicKey);
        registry.add("app.push.vapid-private-key", VAPID::privateKey);
        // Aqui o "serviço de push" é a própria máquina; em produção, só os serviços dos navegadores.
        registry.add("app.push.allowed-origins", () -> "http://127.0.0.1");
    }

    @AfterAll
    static void stop() {
        PUSH.close();
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** O dono, o garçom (com o id dele na equipe), a cozinha e a mesa 04. */
    record Restaurant(String owner, String waiter, String waiterMember, String kitchen, String qr04) {
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        PUSH.received.clear();
        sakura = newRestaurant();
    }

    private Restaurant newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"Sakura Sushi","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(email, PASSWORD), null);
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD), null);
        String owner = json(login, "$.accessToken");
        UUID establishment = UUID.fromString(json(login, "$.establishment.id"));
        // O MockMvc chega como 127.0.0.1: esta é "a rede do salão".
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}", null);
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}", null), "$.id");
        String qr04 = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector), null), "$.publicToken");
        String[] waiter = staff(owner, establishment, "Carlos", Role.STAFF);
        String[] kitchen = staff(owner, establishment, "Kenji", Role.KITCHEN);
        return new Restaurant(owner, waiter[0], waiter[1], kitchen[0], qr04);
    }

    /** O token da pessoa e o id dela na equipe. */
    private String[] staff(String owner, UUID establishment, String name, Role role) throws Exception {
        MvcTestResult added = send("POST", owner, "/api/v1/team-members",
                "{\"name\":\"%s\",\"role\":\"%s\",\"pin\":\"258014\"}".formatted(name, role), null);
        assertThat(added).hasStatus(HttpStatus.CREATED);
        UUID userId = jdbc.queryForObject("SELECT user_id FROM membership WHERE id = ?::uuid", UUID.class,
                json(added, "$.id"));
        String token = tokenIssuer.issue(new TokenSubject(new UserId(userId), new EstablishmentId(establishment), role))
                .value();
        return new String[] {token, json(added, "$.id")};
    }

    private MvcTestResult send(String method, String token, String uri, String body, Cookie cookie) {
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
        if (cookie != null) {
            request = request.cookie(cookie);
        }
        return request.exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), path));
    }

    private MvcTestResult subscribe(String token, String endpoint) {
        String auth = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        return send("POST", token, "/api/v1/push/subscriptions", """
                {"endpoint":"%s","keys":{"p256dh":"%s","auth":"%s"}}
                """.formatted(endpoint, TestVapidKeys.generate().publicKey(), auth), null);
    }

    /** A mesa chama pelo celular (com a presença confirmada pela rede do salão). */
    private void tableCalls(Restaurant r, String type) {
        Cookie phone = mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/presence").cookie(phone).exchange();
        String body = "BILL".equals(type) ? "{\"type\":\"BILL\",\"paymentMethod\":\"PIX\"}" : "{\"type\":\"WAITER\"}";
        assertThat(send("POST", null, "/api/v1/public/tables/" + r.qr04() + "/calls", body, phone))
                .hasStatus(HttpStatus.CREATED);
    }

    /** Os caminhos que receberam notificação (esperando o envio, que é depois do commit e em paralelo). */
    private List<String> received(int expected) throws InterruptedException {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < expected; i++) {
            FakePushService.Received got = PUSH.received.poll(10, TimeUnit.SECONDS);
            assertThat(got).as("a notificação %d de %d chegou", i + 1, expected).isNotNull();
            assertThat(got.headers()).containsEntry("content-encoding", "aes128gcm");
            paths.add(got.path());
        }
        // E nada a mais, depois de um tempo de folga.
        assertThat(PUSH.received.poll(500, TimeUnit.MILLISECONDS)).isNull();
        return paths;
    }

    @Test
    void theCallReachesTheActiveFloorTeam() throws Exception {
        String dono = "dono-" + UUID.randomUUID();
        String carlos = "carlos-" + UUID.randomUUID();
        assertThat(send("GET", sakura.waiter(), "/api/v1/push/settings", null, null)).bodyJson()
                .extractingPath("$.publicKey").isEqualTo(VAPID.publicKey());
        assertThat(subscribe(sakura.owner(), PUSH.endpoint(dono))).hasStatus(HttpStatus.CREATED);
        assertThat(subscribe(sakura.waiter(), PUSH.endpoint(carlos))).hasStatus(HttpStatus.CREATED);

        tableCalls(sakura, "WAITER");
        assertThat(received(2)).containsExactlyInAnyOrder("/" + dono, "/" + carlos);

        // O garçom foi desligado da equipe: o celular dele para de receber.
        assertThat(send("DELETE", sakura.owner(), "/api/v1/team-members/" + sakura.waiterMember(), null, null))
                .hasStatus(HttpStatus.NO_CONTENT);
        tableCalls(sakura, "BILL");
        assertThat(received(1)).containsExactly("/" + dono);
    }

    @Test
    void onlyKnownServicesTheFloorTeamAndYourOwnSubscription() throws Exception {
        String carlos = PUSH.endpoint("carlos-" + UUID.randomUUID());

        assertThat(subscribe(sakura.waiter(), "https://evil.example.com/push")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(subscribe(sakura.kitchen(), PUSH.endpoint("kenji"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(subscribe(null, carlos)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(subscribe(sakura.waiter(), carlos)).hasStatus(HttpStatus.CREATED);

        // Outro restaurante não tira a inscrição do garçom, nem recebe os chamados da mesa daqui.
        Restaurant other = newRestaurant();
        String theirs = "outro-" + UUID.randomUUID();
        assertThat(subscribe(other.owner(), PUSH.endpoint(theirs))).hasStatus(HttpStatus.CREATED);
        assertThat(send("DELETE", other.owner(), "/api/v1/push/subscriptions?endpoint=" + carlos, null, null))
                .hasStatus(HttpStatus.NOT_FOUND);
        tableCalls(sakura, "WAITER");
        assertThat(received(1)).containsExactly(URI.create(carlos).getPath());

        assertThat(send("DELETE", sakura.waiter(), "/api/v1/push/subscriptions?endpoint=" + carlos, null, null))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("DELETE", sakura.waiter(), "/api/v1/push/subscriptions?endpoint=" + carlos, null, null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
