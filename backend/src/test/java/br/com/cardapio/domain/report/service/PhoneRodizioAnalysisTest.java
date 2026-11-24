package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.CardChange;
import br.com.cardapio.domain.report.model.CardTime;
import br.com.cardapio.domain.report.model.CardTrack;
import br.com.cardapio.domain.report.model.DailyOrigin;
import br.com.cardapio.domain.report.model.OriginPoint;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static br.com.cardapio.domain.rodizio.model.TabRodizioStatus.PAUSED;
import static br.com.cardapio.domain.rodizio.model.TabRodizioStatus.SERVING;
import static org.assertj.core.api.Assertions.assertThat;

class PhoneRodizioAnalysisTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final Instant T = Instant.parse("2026-11-22T21:00:00Z");

    private static ReportPeriod days(LocalDate from, int days) {
        return new ReportPeriod(from, from.plusDays(days - 1), SP);
    }

    @Test
    void theGroupingFollowsTheLength() {
        assertThat(PhoneRodizioAnalysis.grouping(days(MONDAY, 14))).isEqualTo(SeriesGrouping.DAY);
        assertThat(PhoneRodizioAnalysis.grouping(days(MONDAY, 30))).isEqualTo(SeriesGrouping.WEEK);
        assertThat(PhoneRodizioAnalysis.grouping(days(MONDAY, 120))).isEqualTo(SeriesGrouping.MONTH);
    }

    @Test
    void daysWithoutOrdersCountZero() {
        List<OriginPoint> points = PhoneRodizioAnalysis.origin(days(MONDAY, 3),
                List.of(new DailyOrigin(MONDAY.plusDays(1), 4, 1)));

        assertThat(points).containsExactly(
                new OriginPoint(MONDAY, MONDAY, 0, 0),
                new OriginPoint(MONDAY.plusDays(1), MONDAY.plusDays(1), 4, 1),
                new OriginPoint(MONDAY.plusDays(2), MONDAY.plusDays(2), 0, 0));
    }

    @Test
    void weeksGoFromMondayToSundayCutAtTheEnds() {
        LocalDate wednesday = MONDAY.plusDays(2);
        List<OriginPoint> points = PhoneRodizioAnalysis.origin(days(wednesday, 30), List.of(
                new DailyOrigin(wednesday, 2, 0), new DailyOrigin(MONDAY.plusDays(7), 3, 1),
                new DailyOrigin(MONDAY.plusDays(13), 1, 1)));

        assertThat(points.getFirst()).isEqualTo(new OriginPoint(wednesday, MONDAY.plusDays(6), 2, 0));
        assertThat(points.get(1)).isEqualTo(new OriginPoint(MONDAY.plusDays(7), MONDAY.plusDays(13), 4, 2));
        assertThat(points.getLast().end()).isEqualTo(wednesday.plusDays(29));
    }

    @Test
    void theCardStartsGreenAndCountsEachColor() {
        CardTime time = PhoneRodizioAnalysis.cardTime(List.of(new CardTrack(T, T.plus(Duration.ofMinutes(60)),
                List.of(new CardChange(PAUSED, T.plus(Duration.ofMinutes(20))),
                        new CardChange(SERVING, T.plus(Duration.ofMinutes(30)))))));

        assertThat(time.serving()).isEqualTo(Duration.ofMinutes(50));
        assertThat(time.paused()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void aCardRedUntilTheEndCountsRed() {
        CardTime time = PhoneRodizioAnalysis.cardTime(List.of(
                new CardTrack(T, T.plus(Duration.ofMinutes(40)),
                        List.of(new CardChange(PAUSED, T.plus(Duration.ofMinutes(10))))),
                new CardTrack(T, T.plus(Duration.ofMinutes(15)), List.of())));

        assertThat(time.serving()).isEqualTo(Duration.ofMinutes(25));
        assertThat(time.paused()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void changesOutsideTheTrackDoNotCount() {
        CardTime time = PhoneRodizioAnalysis.cardTime(List.of(new CardTrack(T, T.plus(Duration.ofMinutes(30)),
                List.of(new CardChange(PAUSED, T.plus(Duration.ofMinutes(45)))))));

        assertThat(time.serving()).isEqualTo(Duration.ofMinutes(30));
        assertThat(time.paused()).isEqualTo(Duration.ZERO);
    }
}
