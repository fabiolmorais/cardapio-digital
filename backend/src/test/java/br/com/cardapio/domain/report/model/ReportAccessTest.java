package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.plan.model.PlanId;
import br.com.cardapio.domain.plan.model.SupportLevel;
import br.com.cardapio.domain.report.exception.ReportPeriodOutOfRangeException;
import br.com.cardapio.domain.report.exception.ReportsNotInPlanException;
import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static br.com.cardapio.domain.plan.model.PlanFeature.BASIC_REPORTS;
import static br.com.cardapio.domain.plan.model.PlanFeature.REPORTS;
import static br.com.cardapio.domain.plan.model.PlanFeature.REPORT_EXPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportAccessTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-21");
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private static Plan plan(int historyDays, PlanFeature... features) {
        return new Plan(new PlanId(UUID.randomUUID()), "plano", "Plano", Money.ofCents(14990), Money.ofCents(149900),
                Limit.of(50), Limit.unlimited(), Limit.of(3), historyDays, Set.of(features), SupportLevel.WHATSAPP, 1);
    }

    private static final ReportAccess ESSENCIAL = ReportAccess.of(plan(90));
    private static final ReportAccess PRO = ReportAccess.of(plan(365, BASIC_REPORTS));
    private static final ReportAccess BUSINESS = ReportAccess.of(plan(0, BASIC_REPORTS, REPORTS, REPORT_EXPORT));

    private static ReportPeriod period(String from, String to) {
        return new ReportPeriod(LocalDate.parse(from), LocalDate.parse(to), SAO_PAULO);
    }

    @Test
    void theLevelFollowsThePlan() {
        assertThat(ESSENCIAL.level()).isEqualTo(ReportLevel.NONE);
        assertThat(PRO.level()).isEqualTo(ReportLevel.BASIC);
        assertThat(BUSINESS.level()).isEqualTo(ReportLevel.FULL);
        assertThat(PRO.canExport()).isFalse();
        assertThat(BUSINESS.canExport()).isTrue();
    }

    @Test
    void fullCoversBasicButBasicDoesNotCoverFull() {
        assertThat(ReportLevel.FULL.covers(ReportLevel.BASIC)).isTrue();
        assertThat(ReportLevel.BASIC.covers(ReportLevel.BASIC)).isTrue();
        assertThat(ReportLevel.BASIC.covers(ReportLevel.FULL)).isFalse();
        assertThat(ReportLevel.NONE.covers(ReportLevel.BASIC)).isFalse();
    }

    @Test
    void theProGoesBackTwelveMonthsAndTheBusinessHasNoLimit() {
        assertThat(PRO.earliestDay(TODAY)).contains(LocalDate.parse("2025-11-22"));
        assertThat(BUSINESS.earliestDay(TODAY)).isEmpty();
    }

    @Test
    void theEssencialHasNoReports() {
        assertThatThrownBy(ESSENCIAL::requireReports)
                .isInstanceOf(ReportsNotInPlanException.class)
                .hasMessageContaining("Pro");
        assertThatThrownBy(() -> ESSENCIAL.checkPeriod(period("2026-11-14", "2026-11-20"), TODAY))
                .isInstanceOf(ReportsNotInPlanException.class);
    }

    @Test
    void onlyTheBusinessExports() {
        assertThatThrownBy(PRO::requireExport)
                .isInstanceOf(ReportsNotInPlanException.class)
                .hasMessageContaining("Business");
        assertThatCode(BUSINESS::requireExport).doesNotThrowAnyException();
    }

    @Test
    void aPeriodInsideTheHistoryIsFine() {
        assertThatCode(() -> PRO.checkPeriod(period("2025-11-22", "2026-11-21"), TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> BUSINESS.checkPeriod(period("2020-01-01", "2020-12-31"), TODAY))
                .doesNotThrowAnyException();
    }

    @Test
    void theProCannotGoBeforeItsHistory() {
        assertThatThrownBy(() -> PRO.checkPeriod(period("2025-11-21", "2025-11-30"), TODAY))
                .isInstanceOf(ReportPeriodOutOfRangeException.class)
                .hasMessageContaining("12 meses");
    }

    @Test
    void noPlanReportsTheFuture() {
        assertThatThrownBy(() -> BUSINESS.checkPeriod(period("2026-11-20", "2026-11-22"), TODAY))
                .isInstanceOf(ReportPeriodOutOfRangeException.class)
                .hasMessageContaining("hoje");
    }
}
