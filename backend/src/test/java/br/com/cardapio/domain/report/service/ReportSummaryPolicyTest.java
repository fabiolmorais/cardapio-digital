package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.PrepTotal;
import br.com.cardapio.domain.report.model.SummaryCoverage;
import br.com.cardapio.domain.report.model.TimeSpan;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReportSummaryPolicyTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private static Instant at(String day, int hour) {
        return LocalDate.parse(day).atTime(hour, 0).atZone(SAO_PAULO).toInstant();
    }

    private static Optional<LocalDate> day(String day) {
        return Optional.of(LocalDate.parse(day));
    }

    @Test
    void withoutSummaryEverythingIsLive() {
        SummaryCoverage coverage = ReportSummaryPolicy.coverage(at("2026-01-01", 0), at("2026-02-01", 0), SAO_PAULO,
                Optional.empty());
        assertThat(coverage.usesSummary()).isFalse();
        assertThat(coverage.live()).containsExactly(new TimeSpan(at("2026-01-01", 0), at("2026-02-01", 0)));
    }

    @Test
    void wholeDaysComeFromTheSummaryAndTheRecentDaysAreLive() {
        // 12 meses até hoje, com o resumo feito até ontem (até = hoje, 25/11).
        SummaryCoverage coverage = ReportSummaryPolicy.coverage(at("2025-11-26", 0), at("2026-11-26", 0), SAO_PAULO,
                day("2026-11-25"));
        assertThat(coverage.from()).isEqualTo(day("2025-11-26"));
        assertThat(coverage.until()).isEqualTo(day("2026-11-25"));
        assertThat(coverage.live()).containsExactly(new TimeSpan(at("2026-11-25", 0), at("2026-11-26", 0)));
    }

    @Test
    void partialDaysAtTheEdgesAreLive() {
        // A comparação de "hoje às 21h": começa à meia-noite e para às 21h de um dia já resumido.
        SummaryCoverage coverage = ReportSummaryPolicy.coverage(at("2026-11-10", 6), at("2026-11-20", 21), SAO_PAULO,
                day("2026-11-25"));
        assertThat(coverage.from()).isEqualTo(day("2026-11-11"));
        assertThat(coverage.until()).isEqualTo(day("2026-11-20"));
        assertThat(coverage.live()).containsExactly(new TimeSpan(at("2026-11-10", 6), at("2026-11-11", 0)),
                new TimeSpan(at("2026-11-20", 0), at("2026-11-20", 21)));
    }

    @Test
    void aPeriodWithoutWholeSummarizedDaysIsLive() {
        // Hoje (ainda não resumido) e um dia pela metade.
        assertThat(ReportSummaryPolicy.coverage(at("2026-11-25", 0), at("2026-11-26", 0), SAO_PAULO,
                day("2026-11-25")).usesSummary()).isFalse();
        assertThat(ReportSummaryPolicy.coverage(at("2026-11-20", 6), at("2026-11-20", 21), SAO_PAULO,
                day("2026-11-25")).usesSummary()).isFalse();
    }

    @Test
    void theTaskRedoesTheLastDaysOrTheWholeHistory() {
        assertThat(ReportSummaryPolicy.rebuildFrom(Optional.empty(), day("2025-03-02"))).isEqualTo(day("2025-03-02"));
        assertThat(ReportSummaryPolicy.rebuildFrom(Optional.empty(), Optional.empty())).isEmpty();
        assertThat(ReportSummaryPolicy.rebuildFrom(day("2026-11-25"), Optional.empty())).isEqualTo(day("2026-11-22"));
        // Nunca antes do primeiro dia com movimento.
        assertThat(ReportSummaryPolicy.rebuildFrom(day("2026-11-25"), day("2026-11-24"))).isEqualTo(day("2026-11-24"));
    }

    @Test
    void theAverageIsRoundedToTheSecondAfterAddingTheParts() {
        ProductId guioza = ProductId.newId();
        PrepTotal summary = new PrepTotal(guioza, "Guioza", 2, 1_000_400);
        PrepTotal live = new PrepTotal(guioza, "Guioza", 1, 500_200);
        var average = summary.plus(live).average();
        assertThat(average.preparations()).isEqualTo(3);
        assertThat(average.average()).isEqualTo(Duration.ofSeconds(500));
    }
}
