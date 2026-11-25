package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.SummaryCoverage;
import br.com.cardapio.domain.report.model.TimeSpan;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * As regras do resumo diário (ADR 0014): o que ele cobre e quais dias a tarefa refaz.
 */
public final class ReportSummaryPolicy {

    /**
     * Quantos dias antes do último resumido a tarefa refaz a cada virada: um item que ficou pronto
     * depois da meia-noite, um ajuste no dia seguinte. Dias mais antigos não mudam.
     */
    public static final int REBUILD_DAYS = 3;

    private ReportSummaryPolicy() {
    }

    /**
     * Divide [início, fim) entre o resumo e as consultas diretas. O resumo só entra com dias inteiros
     * (no fuso do restaurante) e só até onde já foi feito; o resto (as pontas e os dias recentes) vai
     * para as consultas diretas. Sem resumo, tudo é direto.
     */
    public static SummaryCoverage coverage(Instant start, Instant end, ZoneId zone,
                                           Optional<LocalDate> summarizedUntil) {
        List<TimeSpan> whole = start.isBefore(end) ? List.of(new TimeSpan(start, end)) : List.of();
        if (summarizedUntil.isEmpty() || whole.isEmpty()) {
            return new SummaryCoverage(Optional.empty(), Optional.empty(), whole);
        }
        LocalDate first = LocalDate.ofInstant(start, zone);
        if (first.atStartOfDay(zone).toInstant().isBefore(start)) {
            first = first.plusDays(1);
        }
        // O dia do fim só entra se terminar até o fim; como o fim nunca passa do começo do próprio
        // dia, ele fica sempre de fora (é o "até" exclusivo).
        LocalDate last = LocalDate.ofInstant(end, zone);
        LocalDate until = last.isBefore(summarizedUntil.get()) ? last : summarizedUntil.get();
        if (!first.isBefore(until)) {
            return new SummaryCoverage(Optional.empty(), Optional.empty(), whole);
        }
        Instant coveredStart = first.atStartOfDay(zone).toInstant();
        Instant coveredEnd = until.atStartOfDay(zone).toInstant();
        List<TimeSpan> live = new ArrayList<>();
        if (start.isBefore(coveredStart)) {
            live.add(new TimeSpan(start, coveredStart));
        }
        if (coveredEnd.isBefore(end)) {
            live.add(new TimeSpan(coveredEnd, end));
        }
        return new SummaryCoverage(Optional.of(first), Optional.of(until), live);
    }

    /**
     * De que dia a tarefa refaz o resumo: sem resumo, do primeiro dia com movimento; com resumo,
     * {@link #REBUILD_DAYS} antes de onde parou (nunca antes do primeiro dia com movimento, quando
     * se sabe qual é). Sem resumo e sem movimento, não há o que fazer.
     */
    public static Optional<LocalDate> rebuildFrom(Optional<LocalDate> summarizedUntil,
                                                  Optional<LocalDate> firstActivityDay) {
        if (summarizedUntil.isEmpty()) {
            return firstActivityDay;
        }
        LocalDate back = summarizedUntil.get().minusDays(REBUILD_DAYS);
        return Optional.of(firstActivityDay.filter(back::isBefore).orElse(back));
    }
}
