package br.com.cardapio.adapter.in.web.report;

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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ReportControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String CONTEXT = "/api/v1/reports/context";

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
        assertThat(send(null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = send(null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
    }

    private MvcTestResult send(String token, String uri, String body) {
        var request = body == null ? mvc.get().uri(uri)
                : mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    private MvcTestResult context(Owner owner) {
        return send(owner.token(), CONTEXT, null);
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        Object value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value == null ? null : value.toString();
    }

    private static boolean has(MvcTestResult result, String field) throws Exception {
        return JsonPath.<java.util.Map<String, Object>>read(result.getResponse().getContentAsString(), "$")
                .get(field) != null;
    }

    private void onPlan(Owner owner, String code) {
        jdbc.update("""
                UPDATE subscription SET plan_id = (SELECT id FROM plan WHERE code = ?)
                WHERE organization_id = (SELECT organization_id FROM establishment WHERE id = ?)
                """, code, owner.establishmentId());
    }

    /** Uma comanda fechada no instante dado, direto no banco (com o setor e a mesa que ela precisa). */
    private void closedTabAt(Owner owner, Instant closedAt) {
        UUID sector = UUID.randomUUID();
        UUID table = UUID.randomUUID();
        jdbc.update("INSERT INTO sector (id, establishment_id, name, position) VALUES (?, ?, ?, 0)",
                sector, owner.establishmentId(), "Salão " + sector.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO dining_table (id, establishment_id, sector_id, label, public_token, token_issued_at)
                VALUES (?, ?, ?, ?, ?, now())
                """, table, owner.establishmentId(), sector, table.toString().substring(0, 8), UUID.randomUUID().toString());
        jdbc.update("""
                INSERT INTO tab (id, establishment_id, table_id, status, opened_at, closed_at, closed_by,
                                 total_cents, payment_method)
                VALUES (?, ?, ?, 'CLOSED', ?, ?, ?, 4990, 'PIX')
                """, UUID.randomUUID(), owner.establishmentId(), table, Timestamp.from(closedAt.minusSeconds(3600)),
                Timestamp.from(closedAt), owner.userId());
    }

    @Test
    void theProTrialSeesTheBasicReportsTwelveMonthsBack() throws Exception {
        MvcTestResult result = context(sakura);

        assertThat(result).hasStatusOk();
        // Todo cadastro começa no teste grátis do Pro.
        assertThat(json(result, "$.level")).isEqualTo("BASIC");
        assertThat(json(result, "$.canExport")).isEqualTo("false");
        assertThat(json(result, "$.timeZone")).isEqualTo("America/Sao_Paulo");
        LocalDate today = LocalDate.parse(json(result, "$.today"));
        assertThat(today).isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        assertThat(LocalDate.parse(json(result, "$.earliestDay"))).isEqualTo(today.minusDays(364));
        assertThat(json(result, "$.maxPeriodDays")).isEqualTo("366");
        // Restaurante novo: ainda não há dados.
        assertThat(has(result, "firstDataDay")).isFalse();
    }

    @Test
    void theBusinessSeesEverythingWithoutAHistoryLimit() throws Exception {
        onPlan(sakura, "business");

        MvcTestResult result = context(sakura);

        assertThat(json(result, "$.level")).isEqualTo("FULL");
        assertThat(json(result, "$.canExport")).isEqualTo("true");
        assertThat(has(result, "earliestDay")).isFalse();
    }

    @Test
    void theEssencialOpensTheScreenWithoutReports() throws Exception {
        onPlan(sakura, "essential");

        MvcTestResult result = context(sakura);

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.level")).isEqualTo("NONE");
        assertThat(json(result, "$.canExport")).isEqualTo("false");
    }

    @Test
    void theFirstDataDayIsTheFirstClosedTabInTheRestaurantsTimeZone() throws Exception {
        // Sexta, 23h30 em São Paulo (sábado, 2h30 em UTC): o dia do restaurante é a sexta.
        closedTabAt(sakura, Instant.parse("2026-11-14T02:30:00Z"));
        closedTabAt(sakura, Instant.parse("2026-11-18T22:00:00Z"));

        assertThat(json(context(sakura), "$.firstDataDay")).isEqualTo("2026-11-13");
    }

    @Test
    void anotherRestaurantsTabsNeverShowUp() throws Exception {
        Owner other = newRestaurant();
        closedTabAt(other, Instant.parse("2026-10-01T20:00:00Z"));

        assertThat(has(context(sakura), "firstDataDay")).isFalse();
        assertThat(json(context(other), "$.firstDataDay")).isEqualTo("2026-10-01");
    }

    @Test
    void onlyTheOwnerAndTheManagerOpenTheReports() throws Exception {
        String manager = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.MANAGER)).value();
        assertThat(send(manager, CONTEXT, null)).hasStatusOk();
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            String token = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                    new EstablishmentId(sakura.establishmentId()), role)).value();
            assertThat(send(token, CONTEXT, null)).hasStatus(HttpStatus.FORBIDDEN);
        }
        assertThat(send(null, CONTEXT, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
