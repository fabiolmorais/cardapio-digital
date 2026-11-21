package br.com.cardapio.domain.report.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SummaryWindowTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void theDayGoesFromTheSendHourToTheSameHourOfTheNextDay() {
        // Às 2h: a sexta 14 vai das 2h de sexta às 2h de sábado (a madrugada conta na sexta).
        var friday = new SummaryWindow(LocalDate.of(2026, 11, 13), 2, SAO_PAULO);

        assertThat(friday.start()).isEqualTo(Instant.parse("2026-11-13T05:00:00Z"));
        assertThat(friday.end()).isEqualTo(Instant.parse("2026-11-14T05:00:00Z"));
        assertThat(friday.weekBefore().day()).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @Test
    void theLastEndedDayDependsOnTheSendHour() {
        // Sábado 14, 1h59 em São Paulo: a sexta ainda não terminou, o último é a quinta.
        assertThat(SummaryWindow.lastEnded(Instant.parse("2026-11-14T04:59:00Z"), 2, SAO_PAULO).day())
                .isEqualTo(LocalDate.of(2026, 11, 12));
        // Às 2h em ponto, a sexta terminou.
        assertThat(SummaryWindow.lastEnded(Instant.parse("2026-11-14T05:00:00Z"), 2, SAO_PAULO).day())
                .isEqualTo(LocalDate.of(2026, 11, 13));
        // À meia-noite: o dia é o de calendário.
        assertThat(SummaryWindow.lastEnded(Instant.parse("2026-11-14T03:00:00Z"), 0, SAO_PAULO).day())
                .isEqualTo(LocalDate.of(2026, 11, 13));
        // Às 8h, de manhã: sábado às 9h, o último é a sexta (das 8h de sexta às 8h de sábado).
        assertThat(SummaryWindow.lastEnded(Instant.parse("2026-11-14T12:00:00Z"), 8, SAO_PAULO).day())
                .isEqualTo(LocalDate.of(2026, 11, 13));
    }

    @Test
    void theHourGoesFromZeroToTwentyThree() {
        assertThatThrownBy(() -> new SummaryWindow(LocalDate.of(2026, 11, 13), 24, SAO_PAULO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DailySummarySettings(true, true, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(DailySummarySettings.DEFAULT.sendHour()).isEqualTo(2);
        assertThat(new DailySummarySettings(false, false, 2).active()).isFalse();
        assertThat(new DailySummarySettings(false, true, 2).active()).isTrue();
    }
}
