package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.CardChange;
import br.com.cardapio.domain.report.model.CardTime;
import br.com.cardapio.domain.report.model.CardTrack;
import br.com.cardapio.domain.report.model.DailyOrigin;
import br.com.cardapio.domain.report.model.OriginPoint;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** As regras do relatório de celular e rodízio (REL-63, 74). */
public final class PhoneRodizioAnalysis {

    /** As viradas do cartão são gravadas desde a V46 (R5): antes disso, o cartão não tem histórico. */
    public static final Instant CARD_HISTORY_SINCE = Instant.parse("2026-11-21T03:00:00Z");

    /** Quantos produtos aparecem em "Mais pedidos no rodízio" e nos cortes da passagem. */
    public static final int TOP_ITEMS = 5;
    public static final int TOP_CUTS = 3;

    private PhoneRodizioAnalysis() {
    }

    /** Por dia até 2 semanas, por semana (de segunda a domingo) até ~3 meses, depois por mês. */
    public static SeriesGrouping grouping(ReportPeriod period) {
        int days = period.days();
        return days <= 14 ? SeriesGrouping.DAY : days <= 93 ? SeriesGrouping.WEEK : SeriesGrouping.MONTH;
    }

    /** Quem lança os pedidos, em cada dia, semana ou mês do período (os dias sem pedido contam zero). */
    public static List<OriginPoint> origin(ReportPeriod period, Collection<DailyOrigin> days) {
        Function<LocalDate, LocalDate> bucket = switch (grouping(period)) {
            case DAY -> d -> d;
            case WEEK -> d -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> d -> d.withDayOfMonth(1);
        };
        Map<LocalDate, DailyOrigin> byDay = days.stream()
                .collect(Collectors.toMap(DailyOrigin::day, d -> d, (a, b) -> a));
        Map<LocalDate, List<DailyOrigin>> buckets = new LinkedHashMap<>();
        for (LocalDate day : period.dates()) {
            buckets.computeIfAbsent(bucket.apply(day), k -> new ArrayList<>())
                    .add(byDay.getOrDefault(day, new DailyOrigin(day, 0, 0)));
        }
        return buckets.values().stream()
                .map(group -> new OriginPoint(group.getFirst().day(), group.getLast().day(),
                        group.stream().mapToInt(DailyOrigin::phone).sum(),
                        group.stream().mapToInt(DailyOrigin::staff).sum()))
                .toList();
    }

    /**
     * O tempo verde e vermelho dos cartões: cada cartão começa verde no início e muda a cada virada,
     * até o fim (o fechamento da comanda). Viradas fora desse intervalo não contam.
     */
    public static CardTime cardTime(Collection<CardTrack> tracks) {
        Duration serving = Duration.ZERO;
        Duration paused = Duration.ZERO;
        for (CardTrack track : tracks) {
            TabRodizioStatus status = TabRodizioStatus.SERVING;
            Instant from = track.start();
            List<CardChange> changes = track.changes().stream()
                    .filter(c -> !c.at().isBefore(track.start()) && c.at().isBefore(track.end()))
                    .sorted(Comparator.comparing(CardChange::at))
                    .toList();
            for (CardChange change : changes) {
                Duration span = Duration.between(from, change.at());
                if (status == TabRodizioStatus.SERVING) serving = serving.plus(span);
                else paused = paused.plus(span);
                status = change.status();
                from = change.at();
            }
            if (track.end().isAfter(from)) {
                Duration span = Duration.between(from, track.end());
                if (status == TabRodizioStatus.SERVING) serving = serving.plus(span);
                else paused = paused.plus(span);
            }
        }
        return new CardTime(serving, paused);
    }
}
