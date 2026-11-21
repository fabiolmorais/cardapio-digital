package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Os dias de um relatório, no fuso do restaurante, do primeiro ao último dia inclusive (ADR 0013).
 * O banco guarda tudo em UTC: {@link #start()} e {@link #end()} são os instantes que cobrem esses
 * dias do restaurante (o fim é exclusivo: meia-noite do dia seguinte).
 *
 * @param from o primeiro dia
 * @param to   o último dia (inclusive)
 * @param zone o fuso do restaurante
 */
public record ReportPeriod(LocalDate from, LocalDate to, ZoneId zone) {

    /** Até 12 meses de uma vez (o tamanho do histórico do Pro). */
    public static final int MAX_DAYS = 366;

    public ReportPeriod {
        Objects.requireNonNull(from, "início é obrigatório");
        Objects.requireNonNull(to, "fim é obrigatório");
        Objects.requireNonNull(zone, "fuso é obrigatório");
        if (to.isBefore(from)) {
            throw new InvalidValueException("O fim do período vem antes do início");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidValueException("Escolha um período de até 12 meses");
        }
    }

    /** Quantos dias o período tem. */
    public int days() {
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    /** O começo do primeiro dia, no fuso do restaurante (inclusive). */
    public Instant start() {
        return from.atStartOfDay(zone).toInstant();
    }

    /** O começo do dia seguinte ao último, no fuso do restaurante (exclusivo). */
    public Instant end() {
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /** O período de mesmo tamanho logo antes deste: a base das comparações ("▲ 9% vs. anterior"). */
    public ReportPeriod previous() {
        return new ReportPeriod(from.minusDays(days()), from.minusDays(1), zone);
    }

    /** O instante cai dentro do período? */
    public boolean contains(Instant instant) {
        return !instant.isBefore(start()) && instant.isBefore(end());
    }
}
