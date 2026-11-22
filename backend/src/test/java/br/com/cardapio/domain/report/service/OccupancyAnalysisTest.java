package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.report.model.HourShare;
import br.com.cardapio.domain.report.model.Occupancy;
import br.com.cardapio.domain.report.model.OccupiedInterval;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OccupancyAnalysisTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 20);
    private static final DiningTableId A = DiningTableId.newId();
    private static final DiningTableId B = DiningTableId.newId();

    private static Instant at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).atZone(SP).toInstant();
    }

    private static Occupancy occupancy(int tables, OccupiedInterval... intervals) {
        return OccupancyAnalysis.occupancy(List.of(intervals), tables, SP, at(FRIDAY.minusDays(7), 0, 0),
                at(FRIDAY.plusDays(1), 0, 0));
    }

    private static double share(Occupancy occupancy, int hour) {
        return occupancy.byHour().stream().filter(h -> h.hour() == hour).mapToDouble(HourShare::share)
                .findFirst().orElse(0);
    }

    @Test
    void turnoverIsTabsPerTablePerDay() {
        assertThat(OccupancyAnalysis.turnover(30, 5, 3)).contains(2.0);
        assertThat(OccupancyAnalysis.turnover(30, 0, 3)).isEmpty();
    }

    @Test
    void revenuePerOccupiedHour() {
        assertThat(OccupancyAnalysis.perOccupiedHour(Money.ofCents(15000), Duration.ofMinutes(90)))
                .contains(Money.ofCents(10000));
        assertThat(OccupancyAnalysis.perOccupiedHour(Money.ofCents(15000), Duration.ZERO)).isEmpty();
    }

    @Test
    void aTableCountsInEveryHourItTouches() {
        Occupancy result = occupancy(2, new OccupiedInterval(A, at(FRIDAY, 20, 40), at(FRIDAY, 22, 10)));

        assertThat(result.byHour()).extracting(HourShare::hour).containsExactly(20, 21, 22);
        assertThat(share(result, 21)).isEqualTo(0.5);
    }

    @Test
    void theEndIsExclusive() {
        Occupancy result = occupancy(1, new OccupiedInterval(A, at(FRIDAY, 20, 0), at(FRIDAY, 21, 0)));

        assertThat(result.byHour()).extracting(HourShare::hour).containsExactly(20);
    }

    @Test
    void theAverageIsOverTheDaysWithMovement() {
        LocalDate thursday = FRIDAY.minusDays(1);
        Occupancy result = occupancy(2,
                new OccupiedInterval(A, at(FRIDAY, 20, 0), at(FRIDAY, 21, 0)),
                new OccupiedInterval(B, at(FRIDAY, 20, 30), at(FRIDAY, 21, 0)),
                new OccupiedInterval(A, at(thursday, 12, 0), at(thursday, 13, 0)));

        assertThat(share(result, 20)).isEqualTo(0.5);
        assertThat(share(result, 12)).isEqualTo(0.25);
        assertThat(result.peak()).hasValueSatisfying(peak -> {
            assertThat(peak.weekday()).isEqualTo(DayOfWeek.FRIDAY);
            assertThat(peak.hour()).isEqualTo(20);
            assertThat(peak.share()).isEqualTo(1.0);
        });
    }

    @Test
    void theSameTableTwiceInAnHourCountsOnce() {
        Occupancy result = occupancy(2,
                new OccupiedInterval(A, at(FRIDAY, 20, 0), at(FRIDAY, 20, 30)),
                new OccupiedInterval(A, at(FRIDAY, 20, 40), at(FRIDAY, 21, 0)));

        assertThat(share(result, 20)).isEqualTo(0.5);
    }

    @Test
    void aForgottenTabCountsOnlyUpToTheLimit() {
        Occupancy result = occupancy(1, new OccupiedInterval(A, at(FRIDAY.minusDays(1), 23, 0),
                at(FRIDAY, 18, 0)));

        // 23h, 0h, 1h, 2h, 3h e 4h: seis horas, nos dois dias.
        assertThat(result.byHour()).extracting(HourShare::hour).containsExactly(0, 1, 2, 3, 4, 23);
    }

    @Test
    void onlyHoursInsideThePeriodCount() {
        Occupancy result = OccupancyAnalysis.occupancy(
                List.of(new OccupiedInterval(A, at(FRIDAY, 22, 0), at(FRIDAY.plusDays(1), 1, 0))), 1, SP,
                at(FRIDAY, 0, 0), at(FRIDAY.plusDays(1), 0, 0));

        assertThat(result.byHour()).extracting(HourShare::hour).containsExactly(22, 23);
        assertThat(share(result, 23)).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void withoutTablesOrTabsThereIsNothing() {
        assertThat(occupancy(0, new OccupiedInterval(A, at(FRIDAY, 20, 0), at(FRIDAY, 21, 0))).byHour()).isEmpty();
        assertThat(occupancy(3).peak()).isEmpty();
    }
}
