package br.com.cardapio.domain.report.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * O "dia" de um resumo: do horário do envio num dia até o mesmo horário do dia seguinte, no fuso do
 * restaurante. Às 2h, o dia 14 vai das 2h do dia 14 às 2h do dia 15: a madrugada da sexta conta na
 * sexta.
 *
 * @param day o dia em que a janela começa (o que o resumo chama de "seu dia")
 */
public record SummaryWindow(LocalDate day, int hour, ZoneId zone) {

    public SummaryWindow {
        Objects.requireNonNull(day, "dia é obrigatório");
        Objects.requireNonNull(zone, "fuso é obrigatório");
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("a hora vai de 0 a 23");
        }
    }

    /** O último dia que já terminou em {@code now}: antes da hora do envio, ainda é o de anteontem. */
    public static SummaryWindow lastEnded(Instant now, int hour, ZoneId zone) {
        var local = now.atZone(zone);
        LocalDate today = local.toLocalDate();
        boolean endedToday = !local.toLocalTime().isBefore(LocalTime.of(hour, 0));
        return new SummaryWindow(today.minusDays(endedToday ? 1 : 2), hour, zone);
    }

    public Instant start() {
        return day.atTime(hour, 0).atZone(zone).toInstant();
    }

    public Instant end() {
        return day.plusDays(1).atTime(hour, 0).atZone(zone).toInstant();
    }

    /** O mesmo dia da semana anterior, para comparar ("vs. sexta passada"). */
    public SummaryWindow weekBefore() {
        return new SummaryWindow(day.minusWeeks(1), hour, zone);
    }
}
