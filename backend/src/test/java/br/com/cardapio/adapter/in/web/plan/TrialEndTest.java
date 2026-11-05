package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.application.port.in.plan.SendTrialNoticesUseCase;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** O fim do teste grátis: a escolha do plano, a mesa suspensa, o script de ativação e os avisos. */
@IntegrationTest
class TrialEndTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String PLAN_REQUEST = "/api/v1/subscription/plan-request";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SendTrialNoticesUseCase notices;

    record Restaurant(String owner, UUID userId, UUID establishmentId, String slug, String qr) {

        String table(String path) {
            return "/api/v1/public/tables/" + qr + path;
        }
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant();
    }

    private Restaurant newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        MvcTestResult signup = send("POST", null, "/api/v1/signup", """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD));
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = json(login, "$.accessToken");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        return new Restaurant(owner, UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")), json(signup, "$.slug"), qr);
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
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    /** Muda o fim do teste grátis direto no banco: o teste "anda no tempo". */
    private void trialEndsIn(Restaurant r, Duration fromNow) {
        jdbc.update("""
                UPDATE subscription SET trial_ends_at = ?
                WHERE organization_id = (SELECT organization_id FROM establishment WHERE id = ?)
                """, Timestamp.from(Instant.now().plus(fromNow)), r.establishmentId());
    }

    private UUID subscriptionId(Restaurant r) {
        return jdbc.queryForObject("""
                SELECT s.id FROM subscription s JOIN establishment e ON e.organization_id = s.organization_id
                WHERE e.id = ?
                """, UUID.class, r.establishmentId());
    }

    @Test
    void theOwnerChoosesThePlanAndItWaitsForThePayment() throws Exception {
        MvcTestResult chosen = send("PUT", sakura.owner(), PLAN_REQUEST,
                "{\"planCode\":\"essential\",\"billingCycle\":\"ANNUAL\"}");

        assertThat(chosen).hasStatusOk();
        assertThat(json(chosen, "$.request.planName")).isEqualTo("Essencial");
        assertThat(json(chosen, "$.request.billingCycle")).isEqualTo("ANNUAL");
        assertThat(json(chosen, "$.plan.code")).isEqualTo("pro"); // os limites continuam os do teste
        assertThat(json(send("GET", sakura.owner(), "/api/v1/subscription", null), "$.request.planCode"))
                .isEqualTo("essential");

        assertThat(send("PUT", sakura.owner(), PLAN_REQUEST, "{\"planCode\":\"ouro\",\"billingCycle\":\"MONTHLY\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        MvcTestResult enterprise = send("PUT", sakura.owner(), PLAN_REQUEST,
                "{\"planCode\":\"enterprise\",\"billingCycle\":\"MONTHLY\"}");
        assertThat(enterprise).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(enterprise, "$.detail")).isEqualTo("O plano Enterprise é sob consulta. Fale com a gente para combinar.");
        assertThat(send("PUT", sakura.owner(), PLAN_REQUEST, "{\"planCode\":\"pro\"}")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void onlyTheOwnerChoosesAndEachRestaurantSeesOnlyItsOwnChoice() throws Exception {
        String manager = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.MANAGER)).value();
        assertThat(send("PUT", manager, PLAN_REQUEST, "{\"planCode\":\"pro\",\"billingCycle\":\"MONTHLY\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);

        send("PUT", sakura.owner(), PLAN_REQUEST, "{\"planCode\":\"business\",\"billingCycle\":\"MONTHLY\"}");
        Restaurant tanuki = newRestaurant();
        MvcTestResult theirs = send("GET", tanuki.owner(), "/api/v1/subscription", null);
        assertThat(JsonPath.<Object>read(theirs.getResponse().getContentAsString(), "$.request")).isNull();
    }

    @Test
    void afterTheGraceTheTableStopsCallingButTheMenuStillOpens() throws Exception {
        Cookie phone = mvc.post().uri(sakura.table("/session")).exchange().getResponse().getCookie("table_session");

        // Na tolerância (venceu há 3 dias): tudo funciona.
        trialEndsIn(sakura, Duration.ofDays(-3));
        assertThat(json(send("GET", null, sakura.table(""), null), "$.serving")).isEqualTo("true");

        // Passou dos 7 dias: a página abre, mas avisa; chamar dá 422.
        trialEndsIn(sakura, Duration.ofDays(-8));
        assertThat(json(send("GET", null, sakura.table(""), null), "$.serving")).isEqualTo("false");
        MvcTestResult call = mvc.post().uri(sakura.table("/calls")).cookie(phone)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"WAITER\"}").exchange();
        assertThat(call).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(call, "$.detail"))
                .isEqualTo("O atendimento pelo celular está pausado neste restaurante. Chame um garçom.");
        assertThat(json(send("GET", sakura.owner(), "/api/v1/subscription", null), "$.standing")).isEqualTo("SUSPENDED");
    }

    @Test
    void theActivationScriptMovesToTheChosenPlanForAYearAfterTheTrial() throws Exception {
        send("PUT", sakura.owner(), PLAN_REQUEST, "{\"planCode\":\"essential\",\"billingCycle\":\"ANNUAL\"}");
        Instant trialEnd = Instant.parse(json(send("GET", sakura.owner(), "/api/v1/subscription", null),
                "$.coveredUntil"));

        // O mesmo arquivo que rodamos com o psql, trocando a variável :'slug' por um parâmetro.
        String script = Files.readString(Path.of("../docs/operacao/ativar-assinatura.sql")).replace(":'slug'", "?");
        Map<String, Object> result = jdbc.queryForMap(script, sakura.slug());

        assertThat(result.get("plano")).isEqualTo("Essencial");
        assertThat(result.get("ciclo")).isEqualTo("ANNUAL");
        MvcTestResult active = send("GET", sakura.owner(), "/api/v1/subscription", null);
        assertThat(json(active, "$.standing")).isEqualTo("ACTIVE");
        assertThat(json(active, "$.plan.code")).isEqualTo("essential");
        assertThat(Instant.parse(json(active, "$.coveredUntil")))
                .isEqualTo(trialEnd.atZone(ZoneOffset.UTC).plusYears(1).toInstant());
        assertThat(JsonPath.<Object>read(active.getResponse().getContentAsString(), "$.request")).isNull();
    }

    @Test
    void theNoticeOfThreeDaysGoesOutOnce() {
        trialEndsIn(sakura, Duration.ofDays(2));

        notices.sendDue();
        notices.sendDue();

        assertThat(jdbc.queryForList("SELECT kind FROM subscription_notice WHERE subscription_id = ?", String.class,
                subscriptionId(sakura))).containsExactly("ENDS_IN_3_DAYS");
    }
}
