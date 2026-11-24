package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static org.assertj.core.api.Assertions.assertThat;

/** A meta de faturamento (R14): deste mês e do próximo, no Business, e o progresso na Visão geral. */
@IntegrationTest
class RevenueGoalsControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    private final YearMonth month = YearMonth.now(SAO_PAULO);

    private ReportTestData data;
    private Owner sakura;
    private Place salao;

    @BeforeEach
    void sakura() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
    }

    private MvcTestResult put(String token, String body) {
        return mvc.put().uri("/api/v1/reports/goals").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private String goals(Long current, Long next) {
        return """
                {"goals":[{"month":"%s","amountCents":%s},{"month":"%s","amountCents":%s}]}
                """.formatted(month, current, month.plusMonths(1), next);
    }

    @Test
    void onlyTheBusinessHasAGoal() {
        assertThat(data.get(sakura, "/api/v1/reports/goals")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(put(sakura.token(), goals(100000L, null))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void theGoalsOfThisMonthAndTheNext() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult empty = data.get(sakura, "/api/v1/reports/goals");
        assertThat(json(empty, "$.current.month")).isEqualTo(month.toString());
        assertThat(json(empty, "$.current.amountCents")).isNull();
        assertThat(json(empty, "$.next.month")).isEqualTo(month.plusMonths(1).toString());

        assertThat(put(data.token(sakura, Role.STAFF), goals(18000000L, null))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(put(data.token(sakura, Role.MANAGER), goals(18000000L, 20000000L))).hasStatusOk();
        MvcTestResult saved = data.get(sakura, "/api/v1/reports/goals");
        assertThat(json(saved, "$.current.amountCents")).isEqualTo("18000000");
        assertThat(json(saved, "$.next.amountCents")).isEqualTo("20000000");

        // Sem valor, a meta sai.
        assertThat(put(sakura.token(), goals(18000000L, null))).hasStatusOk();
        assertThat(json(data.get(sakura, "/api/v1/reports/goals"), "$.next.amountCents")).isNull();
    }

    @Test
    void otherMonthsAndInvalidAmountsAreRefused() {
        data.onPlan(sakura, "business");

        assertThat(put(sakura.token(), """
                {"goals":[{"month":"%s","amountCents":100}]}
                """.formatted(month.minusMonths(1)))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(sakura.token(), goals(0L, null))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(sakura.token(), goals(10_000_000_001L, null))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theOverviewShowsTheMonthAgainstTheGoal() throws Exception {
        data.onPlan(sakura, "business");
        assertThat(put(sakura.token(), goals(100000L, null))).hasStatusOk();
        Instant now = Instant.now();
        data.closedTab(sakura, salao, now.minus(Duration.ofMinutes(10)), now.minus(Duration.ofMinutes(2)), 25000,
                "PIX", 0);
        LocalDate today = LocalDate.now(SAO_PAULO);

        MvcTestResult overview = data.get(sakura, "/api/v1/reports/overview?from=%s&to=%s".formatted(today, today));

        assertThat(json(overview, "$.full.goal.month")).isEqualTo(month.toString());
        assertThat(json(overview, "$.full.goal.goalCents")).isEqualTo("100000");
        assertThat(json(overview, "$.full.goal.achievedCents")).isEqualTo("25000");
        int daysLeft = month.lengthOfMonth() - today.getDayOfMonth() + 1;
        assertThat(json(overview, "$.full.goal.daysLeft")).isEqualTo(String.valueOf(daysLeft));
        assertThat(json(overview, "$.full.goal.perDayNeededCents"))
                .isEqualTo(String.valueOf((75000 + daysLeft - 1) / daysLeft));
    }

    @Test
    void withoutAGoalTheOverviewHasNone() throws Exception {
        data.onPlan(sakura, "business");
        LocalDate today = LocalDate.now(SAO_PAULO);

        MvcTestResult overview = data.get(sakura, "/api/v1/reports/overview?from=%s&to=%s".formatted(today, today));

        assertThat(json(overview, "$.full.goal")).isNull();
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        assertThat(put(other.token(), goals(50000000L, 60000000L))).hasStatusOk();

        assertThat(json(data.get(sakura, "/api/v1/reports/goals"), "$.current.amountCents")).isNull();
        assertThat(json(data.get(other, "/api/v1/reports/goals"), "$.current.amountCents")).isEqualTo("50000000");
    }
}
