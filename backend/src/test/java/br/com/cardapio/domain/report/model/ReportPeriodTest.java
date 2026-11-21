package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportPeriodTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private static ReportPeriod period(String from, String to) {
        return new ReportPeriod(LocalDate.parse(from), LocalDate.parse(to), SAO_PAULO);
    }

    @Test
    void theDaysAreTheRestaurantsDaysNotUtc() {
        ReportPeriod week = period("2026-11-13", "2026-11-19");

        // Meia-noite em São Paulo (UTC-3) é 3h em UTC; o fim é a meia-noite do dia seguinte.
        assertThat(week.start()).isEqualTo(Instant.parse("2026-11-13T03:00:00Z"));
        assertThat(week.end()).isEqualTo(Instant.parse("2026-11-20T03:00:00Z"));
        assertThat(week.days()).isEqualTo(7);
    }

    @Test
    void aTabClosedAt1amBelongsToThatDayInTheRestaurant() {
        ReportPeriod friday = period("2026-11-13", "2026-11-13");

        // Sexta, 23h50 em São Paulo ainda é sexta; sábado 0h10 já não é.
        assertThat(friday.contains(Instant.parse("2026-11-14T02:50:00Z"))).isTrue();
        assertThat(friday.contains(Instant.parse("2026-11-14T03:10:00Z"))).isFalse();
        assertThat(friday.contains(Instant.parse("2026-11-13T02:59:59Z"))).isFalse();
    }

    @Test
    void thePreviousPeriodHasTheSameSizeAndEndsTheDayBefore() {
        ReportPeriod previous = period("2026-11-13", "2026-11-19").previous();

        assertThat(previous.from()).isEqualTo(LocalDate.parse("2026-11-06"));
        assertThat(previous.to()).isEqualTo(LocalDate.parse("2026-11-12"));
        assertThat(period("2026-11-20", "2026-11-20").previous().from()).isEqualTo(LocalDate.parse("2026-11-19"));
    }

    @Test
    void theEndCannotComeBeforeTheStart() {
        assertThatThrownBy(() -> period("2026-11-19", "2026-11-13"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("antes do início");
    }

    @Test
    void atMostTwelveMonthsAtOnce() {
        assertThat(period("2025-11-20", "2026-11-20").days()).isEqualTo(366);
        assertThatThrownBy(() -> period("2025-11-19", "2026-11-20"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("12 meses");
    }

    @Test
    void oneDayComparesWithTheSameWeekdayOfTheWeekBefore() {
        ReportPeriod friday = period("2026-11-20", "2026-11-20");

        assertThat(friday.comparison()).isEqualTo(period("2026-11-13", "2026-11-13"));
        assertThat(period("2026-11-13", "2026-11-19").comparison()).isEqualTo(period("2026-11-06", "2026-11-12"));
    }

    @Test
    void theDatesGoFromTheFirstToTheLastDay() {
        assertThat(period("2026-11-29", "2026-12-01").dates()).containsExactly(LocalDate.parse("2026-11-29"),
                LocalDate.parse("2026-11-30"), LocalDate.parse("2026-12-01"));
    }
}
