package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.DailyRevenue;
import br.com.cardapio.domain.report.model.DayComparison;
import br.com.cardapio.domain.report.model.HourAverage;
import br.com.cardapio.domain.report.model.HourlyRevenue;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.report.model.SeriesPoint;
import br.com.cardapio.domain.report.model.WeekdayHourAverage;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesAnalysisTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private static ReportPeriod period(String from, String to) {
        return new ReportPeriod(LocalDate.parse(from), LocalDate.parse(to), SAO_PAULO);
    }

    private static DailyRevenue day(String day, long cents, int tabs) {
        return new DailyRevenue(LocalDate.parse(day), Money.ofCents(cents), tabs);
    }

    @Test
    void daysWithoutSalesBecomeZeroSoTheChartDoesNotSkipThem() {
        List<DailyRevenue> days = SalesAnalysis.everyDay(period("2026-11-16", "2026-11-18"),
                List.of(day("2026-11-17", 5000, 2)));

        assertThat(days).extracting(d -> d.revenue().cents()).containsExactly(0L, 5000L, 0L);
        assertThat(days.getFirst().day()).isEqualTo(LocalDate.parse("2026-11-16"));
    }

    @Test
    void eachDayIsComparedWithTheMatchingDayOfTheComparison() {
        ReportPeriod week = period("2026-11-13", "2026-11-19");

        List<DayComparison> days = SalesAnalysis.compareDays(week, List.of(day("2026-11-14", 11240, 90)),
                week.comparison(), List.of(day("2026-11-07", 10380, 85)));

        assertThat(days).hasSize(7);
        DayComparison saturday = days.get(1);
        assertThat(saturday.day()).isEqualTo(LocalDate.parse("2026-11-14"));
        assertThat(saturday.comparedDay()).isEqualTo(LocalDate.parse("2026-11-07"));
        assertThat(saturday.revenue().cents()).isEqualTo(11240);
        assertThat(saturday.comparedRevenue().cents()).isEqualTo(10380);
    }

    @Test
    void theHeatmapAveragesByHowManyTimesEachWeekdayHappened() {
        // 1 a 30 de novembro de 2026: 4 sextas (6, 13, 20, 27) e 5 domingos (1, 8, 15, 22, 29).
        ReportPeriod november = period("2026-11-01", "2026-11-30");

        List<WeekdayHourAverage> cells = SalesAnalysis.weekdayHourAverages(november, List.of(
                new HourlyRevenue(5, 21, Money.ofCents(40000), 20),
                new HourlyRevenue(7, 12, Money.ofCents(50000), 25)));

        assertThat(cells).containsExactly(
                new WeekdayHourAverage(5, 21, Money.ofCents(10000)),
                new WeekdayHourAverage(7, 12, Money.ofCents(10000)));
    }

    @Test
    void theHourlyViewIsAnAveragePerDayOfThePeriod() {
        ReportPeriod week = period("2026-11-13", "2026-11-19");

        List<HourAverage> hours = SalesAnalysis.hourAverages(week, List.of(
                new HourlyRevenue(5, 21, Money.ofCents(7000), 4),
                new HourlyRevenue(6, 21, Money.ofCents(7000), 3),
                new HourlyRevenue(6, 12, Money.ofCents(1400), 1)));

        assertThat(hours).containsExactly(
                new HourAverage(12, Money.ofCents(200), new BigDecimal("0.1")),
                new HourAverage(21, Money.ofCents(2000), new BigDecimal("1.0")));
        assertThat(SalesAnalysis.peakHour(hours)).contains(21);
        assertThat(SalesAnalysis.peakHour(List.of())).isEmpty();
    }

    @Test
    void theBestDayIsTheOneThatSoldTheMostAndTheFirstOnATie() {
        List<DailyRevenue> days = List.of(day("2026-11-13", 9870, 80), day("2026-11-14", 11240, 90),
                day("2026-11-15", 11240, 85));

        assertThat(SalesAnalysis.bestDay(days)).map(DailyRevenue::day).contains(LocalDate.parse("2026-11-14"));
        assertThat(SalesAnalysis.bestDay(List.of(day("2026-11-13", 0, 0)))).isEmpty();
    }

    @Test
    void tabsPerDayHasOneDecimal() {
        assertThat(SalesAnalysis.tabsPerDay(period("2026-11-13", "2026-11-19"), 412)).isEqualByComparingTo("58.9");
    }

    @Test
    void weeksStartOnMondayAndAreCutAtTheEdgesOfThePeriod() {
        // Quinta 12 a quarta 25 de novembro: semana cortada (12 a 15), uma inteira (16 a 22) e o resto (23 a 25).
        List<SeriesPoint> weeks = SalesAnalysis.series(period("2026-11-12", "2026-11-25"),
                List.of(day("2026-11-14", 1000, 1), day("2026-11-16", 2000, 2), day("2026-11-22", 3000, 3),
                        day("2026-11-25", 4000, 4)),
                SeriesGrouping.WEEK);

        assertThat(weeks).containsExactly(
                new SeriesPoint(LocalDate.parse("2026-11-12"), LocalDate.parse("2026-11-15"), Money.ofCents(1000), 1),
                new SeriesPoint(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-22"), Money.ofCents(5000), 5),
                new SeriesPoint(LocalDate.parse("2026-11-23"), LocalDate.parse("2026-11-25"), Money.ofCents(4000), 4));
    }

    @Test
    void monthsAreCutAtTheEdgesOfThePeriodToo() {
        List<SeriesPoint> months = SalesAnalysis.series(period("2026-10-20", "2026-11-05"),
                List.of(day("2026-10-31", 1000, 1), day("2026-11-01", 2000, 2)), SeriesGrouping.MONTH);

        assertThat(months).extracting(SeriesPoint::start, SeriesPoint::end).containsExactly(
                org.assertj.core.groups.Tuple.tuple(LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-31")),
                org.assertj.core.groups.Tuple.tuple(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-05")));
        assertThat(SalesAnalysis.series(period("2026-11-01", "2026-11-03"), List.of(), SeriesGrouping.DAY)).hasSize(3);
    }
}
