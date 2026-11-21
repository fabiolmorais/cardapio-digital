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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * As contas dos relatórios de vendas (REL-01, 05, 08), sobre o que o banco já somou. O banco devolve
 * só os dias e as horas que tiveram comandas: aqui os vazios viram zero, para o gráfico não pular dia.
 */
public final class SalesAnalysis {

    private SalesAnalysis() {
    }

    /** Todos os dias do período, com zero onde não houve venda. */
    public static List<DailyRevenue> everyDay(ReportPeriod period, List<DailyRevenue> days) {
        Map<LocalDate, DailyRevenue> byDay = days.stream()
                .collect(Collectors.toMap(DailyRevenue::day, Function.identity(), (a, b) -> a));
        return period.dates().stream()
                .map(day -> byDay.getOrDefault(day, new DailyRevenue(day, Money.ZERO, 0)))
                .toList();
    }

    /** Cada dia do período ao lado do dia correspondente da comparação (o 1º com o 1º). */
    public static List<DayComparison> compareDays(ReportPeriod period, List<DailyRevenue> current,
                                                  ReportPeriod comparison, List<DailyRevenue> compared) {
        List<DailyRevenue> now = everyDay(period, current);
        List<DailyRevenue> before = everyDay(comparison, compared);
        List<DayComparison> result = new ArrayList<>(now.size());
        for (int i = 0; i < now.size(); i++) {
            DailyRevenue other = i < before.size() ? before.get(i) : null;
            result.add(new DayComparison(now.get(i).day(), now.get(i).revenue(),
                    other == null ? null : other.day(), other == null ? Money.ZERO : other.revenue()));
        }
        return result;
    }

    /**
     * O mapa de calor (REL-05): para cada dia da semana e hora com venda, a média por ocorrência
     * daquele dia da semana no período (4 sextas no mês: soma das sextas ÷ 4).
     */
    public static List<WeekdayHourAverage> weekdayHourAverages(ReportPeriod period, List<HourlyRevenue> hours) {
        Map<Integer, Long> occurrences = period.dates().stream()
                .collect(Collectors.groupingBy(d -> d.getDayOfWeek().getValue(), Collectors.counting()));
        return hours.stream()
                .filter(h -> occurrences.containsKey(h.isoWeekday()))
                .map(h -> new WeekdayHourAverage(h.isoWeekday(), h.hour(),
                        Money.ofCents(Math.round((double) h.revenue().cents() / occurrences.get(h.isoWeekday())))))
                .sorted(Comparator.comparingInt(WeekdayHourAverage::isoWeekday)
                        .thenComparingInt(WeekdayHourAverage::hour))
                .toList();
    }

    /** O "por hora do dia" (REL-05, 08): média por dia do período, só das horas com movimento. */
    public static List<HourAverage> hourAverages(ReportPeriod period, List<HourlyRevenue> hours) {
        Map<Integer, List<HourlyRevenue>> byHour = hours.stream()
                .collect(Collectors.groupingBy(HourlyRevenue::hour));
        BigDecimal days = BigDecimal.valueOf(period.days());
        return byHour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    long revenue = e.getValue().stream().mapToLong(h -> h.revenue().cents()).sum();
                    int tabs = e.getValue().stream().mapToInt(HourlyRevenue::tabs).sum();
                    return new HourAverage(e.getKey(), Money.ofCents(Math.round((double) revenue / period.days())),
                            BigDecimal.valueOf(tabs).divide(days, 1, RoundingMode.HALF_UP));
                })
                .toList();
    }

    /** A hora com mais faturamento médio por dia; vazio sem vendas. */
    public static Optional<Integer> peakHour(List<HourAverage> hours) {
        return hours.stream()
                .filter(h -> !h.revenuePerDay().isZero())
                .max(Comparator.comparingLong((HourAverage h) -> h.revenuePerDay().cents())
                        .thenComparing(HourAverage::hour, Comparator.reverseOrder()))
                .map(HourAverage::hour);
    }

    /** O dia que mais faturou (no empate, o primeiro); vazio sem vendas. */
    public static Optional<DailyRevenue> bestDay(List<DailyRevenue> days) {
        DailyRevenue best = null;
        for (DailyRevenue day : days) {
            if (day.revenue().cents() > 0 && (best == null || day.revenue().cents() > best.revenue().cents())) {
                best = day;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Quantas comandas por dia, em média, com uma casa decimal. */
    public static BigDecimal tabsPerDay(ReportPeriod period, int tabs) {
        return BigDecimal.valueOf(tabs).divide(BigDecimal.valueOf(period.days()), 1, RoundingMode.HALF_UP);
    }

    /**
     * O faturamento no tempo (REL-01): por dia, por semana (de segunda a domingo) ou por mês. A primeira
     * e a última semana (ou mês) são cortadas nas pontas do período.
     */
    public static List<SeriesPoint> series(ReportPeriod period, List<DailyRevenue> days, SeriesGrouping grouping) {
        Function<LocalDate, LocalDate> bucket = switch (grouping) {
            case DAY -> d -> d;
            case WEEK -> d -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> d -> d.withDayOfMonth(1);
        };
        Map<LocalDate, List<DailyRevenue>> buckets = new LinkedHashMap<>();
        for (DailyRevenue day : everyDay(period, days)) {
            buckets.computeIfAbsent(bucket.apply(day.day()), k -> new ArrayList<>()).add(day);
        }
        return buckets.values().stream()
                .map(group -> new SeriesPoint(group.getFirst().day(), group.getLast().day(),
                        Money.ofCents(group.stream().mapToLong(d -> d.revenue().cents()).sum()),
                        group.stream().mapToInt(DailyRevenue::tabs).sum()))
                .toList();
    }
}
