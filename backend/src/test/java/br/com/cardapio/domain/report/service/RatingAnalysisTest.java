package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.RatingDay;
import br.com.cardapio.domain.report.model.RatingPoint;
import br.com.cardapio.domain.report.model.ReportPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A nota nos relatórios (REL-55, 83). */
class RatingAnalysisTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate FIRST = LocalDate.of(2026, 11, 1);

    @Test
    void theWaiterNeedsFiveRatings() {
        assertThat(RatingAnalysis.shownAverage(4, 20)).isNull();
        assertThat(RatingAnalysis.shownAverage(5, 23)).isEqualTo(4.6);
        assertThat(RatingAnalysis.shownAverage(3, 0)).isNull();
    }

    @Test
    void aWeekGoesDayByDay() {
        ReportPeriod week = new ReportPeriod(FIRST, FIRST.plusDays(6), SAO_PAULO);

        List<RatingPoint> trend = RatingAnalysis.trend(week, List.of(new RatingDay(FIRST, 2, 9),
                new RatingDay(FIRST.plusDays(6), 3, 11)));

        assertThat(trend).hasSize(7);
        assertThat(trend.getFirst()).isEqualTo(new RatingPoint(FIRST, FIRST, 2, 4.5));
        assertThat(trend.get(1).average()).isNull();
        assertThat(trend.getLast().average()).isEqualTo(3.7);
    }

    @Test
    void aMonthGoesWeekByWeekAndTheLastWeekEndsWithThePeriod() {
        ReportPeriod month = new ReportPeriod(FIRST, FIRST.plusDays(29), SAO_PAULO);

        List<RatingPoint> trend = RatingAnalysis.trend(month, List.of(new RatingDay(FIRST.plusDays(1), 1, 5),
                new RatingDay(FIRST.plusDays(5), 1, 3), new RatingDay(FIRST.plusDays(29), 2, 6)));

        assertThat(trend).hasSize(5);
        assertThat(trend.getFirst()).isEqualTo(new RatingPoint(FIRST, FIRST.plusDays(6), 2, 4.0));
        assertThat(trend.getLast()).isEqualTo(new RatingPoint(FIRST.plusDays(28), FIRST.plusDays(29), 2, 3.0));
        assertThat(trend.get(2).count()).isZero();
    }
}
