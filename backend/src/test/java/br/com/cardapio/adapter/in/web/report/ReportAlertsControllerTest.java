package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Prep;
import br.com.cardapio.application.port.in.report.SendReportAlertsUseCase;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.support.FakePushService;
import br.com.cardapio.support.IntegrationTest;
import br.com.cardapio.support.TestVapidKeys;
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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os alertas em tempo real (R13): a configuração (Business, dono e gerente) e o envio por push para o
 * celular do dono, no máximo um de cada tipo a cada 15 min e só pelo que passou do limite depois do último.
 */
@IntegrationTest
class ReportAlertsControllerTest {

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
        registry.add("app.push.allowed-origins", () -> "http://127.0.0.1");
        // A tarefa agendada não roda sozinha durante o teste: o teste chama a conferência.
        registry.add("app.reports.alerts-interval", () -> "PT10H");
    }

    @AfterAll
    static void stop() {
        PUSH.close();
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SendReportAlertsUseCase alerts;

    private ReportTestData data;
    private Owner sakura;
    private Place salao;
    private UUID prato;
    private String device;

    @BeforeEach
    void sakuraWithTheOwnerPhone() throws Exception {
        PUSH.received.clear();
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        prato = data.product(sakura, "Prato da casa", 2500);
        device = "dono-" + UUID.randomUUID();
        assertThat(subscribe(sakura.token(), PUSH.endpoint(device))).hasStatus(HttpStatus.CREATED);
    }

    private MvcTestResult subscribe(String token, String endpoint) {
        String auth = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        return mvc.post().uri("/api/v1/push/subscriptions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"endpoint":"%s","keys":{"p256dh":"%s","auth":"%s"}}
                        """.formatted(endpoint, TestVapidKeys.generate().publicKey(), auth))
                .exchange();
    }

    private MvcTestResult put(String token, String body) {
        return mvc.put().uri("/api/v1/reports/alerts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private int check(Owner owner) {
        return alerts.check(new EstablishmentId(owner.establishmentId()));
    }

    /** Um item na cozinha desde {@code minutesAgo}, ainda em preparo. */
    private void cooking(int minutesAgo) {
        data.kitchenOrder(sakura, salao, Instant.now().minus(Duration.ofMinutes(minutesAgo)), null,
                new Prep(prato, "Prato da casa", null, false));
    }

    private long pushesToTheOwner() {
        return PUSH.received.stream().filter(r -> r.path().equals("/" + device)).count();
    }

    private void lastSent(String type, Duration ago) {
        jdbc.update("""
                INSERT INTO report_alert_sent (establishment_id, alert_type, sent_at) VALUES (?, ?, ?)
                ON CONFLICT (establishment_id, alert_type) DO UPDATE SET sent_at = EXCLUDED.sent_at
                """, sakura.establishmentId(), type, Timestamp.from(Instant.now().minus(ago)));
    }

    @Test
    void theSettingsAreFromTheBusiness() throws Exception {
        assertThat(data.get(sakura, "/api/v1/reports/alerts")).hasStatus(HttpStatus.FORBIDDEN);
        data.onPlan(sakura, "business");

        MvcTestResult defaults = data.get(sakura, "/api/v1/reports/alerts");

        assertThat(json(defaults, "$.kitchenEnabled")).isEqualTo("true");
        assertThat(json(defaults, "$.kitchenMinutes")).isEqualTo("25");
        assertThat(json(defaults, "$.callMinutes")).isEqualTo("3");
        assertThat(json(defaults, "$.tabEnabled")).isEqualTo("false");
        assertThat(json(defaults, "$.tabHours")).isEqualTo("6");
    }

    @Test
    void theOwnerAndTheManagerChangeThem() throws Exception {
        data.onPlan(sakura, "business");
        String body = """
                {"kitchenEnabled":false,"kitchenMinutes":30,"callEnabled":true,"callMinutes":5,
                 "tabEnabled":true,"tabHours":4}
                """;

        assertThat(put(data.token(sakura, Role.STAFF), body)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(put(data.token(sakura, Role.MANAGER), body)).hasStatusOk();
        assertThat(put(sakura.token(), body.replace("\"kitchenMinutes\":30", "\"kitchenMinutes\":2")))
                .hasStatus(HttpStatus.BAD_REQUEST);

        MvcTestResult saved = data.get(sakura, "/api/v1/reports/alerts");
        assertThat(json(saved, "$.kitchenEnabled")).isEqualTo("false");
        assertThat(json(saved, "$.kitchenMinutes")).isEqualTo("30");
        assertThat(json(saved, "$.tabHours")).isEqualTo("4");
    }

    @Test
    void aLateKitchenWarnsTheOwnerOnceIn15Minutes() {
        data.onPlan(sakura, "business");
        cooking(30);

        assertThat(check(sakura)).isEqualTo(1);
        assertThat(pushesToTheOwner()).isEqualTo(1);
        // Logo depois, mesmo com outro item atrasando, espera os 15 min.
        cooking(26);
        assertThat(check(sakura)).isZero();
    }

    @Test
    void anUnansweredCallAndALongTabWarn() throws Exception {
        data.onPlan(sakura, "business");
        assertThat(put(sakura.token(), """
                {"kitchenEnabled":true,"kitchenMinutes":25,"callEnabled":true,"callMinutes":3,
                 "tabEnabled":true,"tabHours":6}
                """)).hasStatusOk();
        UUID phone = data.session(sakura, salao, Instant.now().minus(Duration.ofMinutes(10)));
        data.call(sakura, salao, phone, Instant.now().minus(Duration.ofMinutes(4)), null, null);
        data.openTab(sakura, salao, Instant.now().minus(Duration.ofMinutes(365)));

        assertThat(check(sakura)).isEqualTo(2);
        assertThat(pushesToTheOwner()).isEqualTo(2);
    }

    @Test
    void onlyWhatCrossedTheLimitAfterTheLastAlertCounts() {
        data.onPlan(sakura, "business");
        // Passou do limite há 35 min; o último alerta foi há 20 min: já avisado.
        cooking(60);
        lastSent("KITCHEN", Duration.ofMinutes(20));
        assertThat(check(sakura)).isZero();

        // Um item novo passou do limite há 5 min, depois do último alerta: avisa.
        cooking(30);
        assertThat(check(sakura)).isEqualTo(1);
    }

    @Test
    void turnedOffOrOutsideTheBusinessNothingIsSent() throws Exception {
        cooking(30);
        assertThat(check(sakura)).isZero();

        data.onPlan(sakura, "business");
        assertThat(put(sakura.token(), """
                {"kitchenEnabled":false,"kitchenMinutes":25,"callEnabled":true,"callMinutes":3,
                 "tabEnabled":false,"tabHours":6}
                """)).hasStatusOk();
        assertThat(check(sakura)).isZero();
        assertThat(pushesToTheOwner()).isZero();
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Varanda");
        UUID dish = data.product(other, "Lamen", 3000);
        data.kitchenOrder(other, theirs, Instant.now().minus(Duration.ofMinutes(15)), null,
                new Prep(dish, "Lamen", null, false));
        assertThat(put(other.token(), """
                {"kitchenEnabled":true,"kitchenMinutes":10,"callEnabled":true,"callMinutes":3,
                 "tabEnabled":false,"tabHours":6}
                """)).hasStatusOk();

        assertThat(check(sakura)).isZero();
        assertThat(pushesToTheOwner()).isZero();
        assertThat(json(data.get(sakura, "/api/v1/reports/alerts"), "$.kitchenMinutes")).isEqualTo("25");
        assertThat(check(other)).isEqualTo(1);
        assertThat(pushesToTheOwner()).isZero();
    }
}
