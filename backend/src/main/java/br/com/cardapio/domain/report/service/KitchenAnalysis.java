package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.CategoryPrep;
import br.com.cardapio.domain.report.model.DelayedSlot;
import br.com.cardapio.domain.report.model.HourPeak;
import br.com.cardapio.domain.report.model.HourTimes;
import br.com.cardapio.domain.report.model.KitchenItem;
import br.com.cardapio.domain.report.model.PeakAverage;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.model.ServiceTimes;
import br.com.cardapio.domain.report.model.SlowItem;
import br.com.cardapio.domain.report.model.WeekdayHour;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** As regras do relatório da cozinha (REL-30). */
public final class KitchenAnalysis {

    /** Um item só entra em "Itens que mais demoram" com pelo menos 5 preparos: menos que isso é sorte ou azar. */
    public static final int MIN_PREPARATIONS = 5;

    /** E o pior horário dele só conta horas com pelo menos 3 preparos. */
    public static final int MIN_PREPARATIONS_IN_HOUR = 3;

    /** Quantos itens aparecem em "Itens que mais demoram". */
    public static final int SLOWEST = 5;

    private KitchenAnalysis() {
    }

    /** O preparo médio de todos os itens, arredondado para o segundo; vazio sem nenhum preparo. */
    public static Optional<Duration> average(Collection<PrepTime> times) {
        if (times.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(averageOf(times.stream().map(PrepTime::time).toList()));
    }

    /**
     * "9 em 10 até" (percentil 90 pelo posto mais próximo): o menor tempo que pelo menos 90% dos preparos
     * não passaram. Mostra o pior caso sem que um pedido esquecido puxe o número, como o máximo puxaria.
     */
    public static Duration ninetyPercent(List<Duration> times) {
        if (times.isEmpty()) {
            throw new IllegalArgumentException("sem preparos");
        }
        List<Duration> sorted = times.stream().sorted().toList();
        int rank = (int) Math.ceil(0.9 * sorted.size());
        return sorted.get(rank - 1);
    }

    /**
     * Os itens que mais demoram: só os que ficaram prontos {@link #MIN_PREPARATIONS} vezes ou mais, do
     * maior "9 em 10 até" para o menor (empate: a média maior, depois o nome).
     */
    public static List<SlowItem> slowest(Collection<PrepTime> times, int limit) {
        Map<ProductId, List<PrepTime>> byProduct = times.stream()
                .collect(Collectors.groupingBy(PrepTime::productId, LinkedHashMap::new, Collectors.toList()));
        return byProduct.values().stream()
                .filter(list -> list.size() >= MIN_PREPARATIONS)
                .map(KitchenAnalysis::item)
                .sorted(Comparator.comparing(SlowItem::ninetyPercent).reversed()
                        .thenComparing(Comparator.comparing(SlowItem::average).reversed())
                        .thenComparing(SlowItem::name))
                .limit(limit)
                .toList();
    }

    /**
     * A hora da semana em que o item mais demora: a de maior preparo médio entre as que tiveram
     * {@link #MIN_PREPARATIONS_IN_HOUR} preparos ou mais (empate: a de mais preparos, depois a primeira
     * da semana).
     */
    public static Optional<WeekdayHour> worstHour(Collection<PrepTime> times) {
        Map<WeekdayHour, List<Duration>> byHour = times.stream().collect(Collectors.groupingBy(
                t -> new WeekdayHour(t.isoWeekday(), t.hour()),
                Collectors.mapping(PrepTime::time, Collectors.toList())));
        return byHour.entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_PREPARATIONS_IN_HOUR)
                .min(Comparator.<Map.Entry<WeekdayHour, List<Duration>>, Duration>comparing(
                                e -> averageOf(e.getValue()), Comparator.reverseOrder())
                        .thenComparing(e -> e.getValue().size(), Comparator.reverseOrder())
                        .thenComparing(e -> e.getKey().isoWeekday())
                        .thenComparing(e -> e.getKey().hour()))
                .map(Map.Entry::getKey);
    }

    private static SlowItem item(List<PrepTime> times) {
        PrepTime first = times.getFirst();
        List<Duration> durations = times.stream().map(PrepTime::time).toList();
        return new SlowItem(first.productId(), first.name(), first.category(), times.size(), averageOf(durations),
                ninetyPercent(durations), worstHour(times));
    }

    private static Duration averageOf(List<Duration> times) {
        long millis = times.stream().mapToLong(Duration::toMillis).sum();
        return Duration.ofSeconds(Math.round(millis / 1000.0 / times.size()));
    }

    /** Pronto e parado no passe mais que isto: "demorou a chegar" (REL-37). */
    public static final Duration LATE_PASSE = Duration.ofMinutes(5);

    /** Pedido esperando a confirmação do garçom mais que isto (REL-35). */
    public static final Duration LATE_CONFIRMATION = Duration.ofMinutes(5);

    /** Quantas horas em "Prontos que demoraram a chegar". */
    public static final int DELAYED_SLOTS = 3;

    /**
     * A média do pico de cada hora do dia (REL-34): para cada hora, a média dos máximos de itens em
     * preparo ao mesmo tempo nos dias em que a cozinha trabalhou nela, com uma casa.
     */
    public static List<PeakAverage> peakAverages(Collection<HourPeak> peaks) {
        return peaks.stream()
                .collect(Collectors.groupingBy(HourPeak::hour, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new PeakAverage(e.getKey(), BigDecimal.valueOf(e.getValue().stream()
                                .mapToInt(HourPeak::peak).sum())
                        .divide(BigDecimal.valueOf(e.getValue().size()), 1, RoundingMode.HALF_UP)))
                .toList();
    }

    /** Os preparos (os itens prontos), com o dia da semana e a hora da chegada no fuso. */
    public static List<PrepTime> prepTimes(Collection<KitchenItem> items, ZoneId zone) {
        return items.stream()
                .filter(i -> i.ready() != null)
                .map(i -> {
                    var local = i.received().atZone(zone);
                    return new PrepTime(i.productId(), i.name(), i.category(), local.getDayOfWeek().getValue(),
                            local.getHour(), positive(Duration.between(i.received(), i.ready())));
                })
                .toList();
    }

    /**
     * O passe (de pronto até entregue) e o tempo até a mesa (da chegada até entregue), em média, dos
     * itens entregues (REL-31, 32), com os pedidos que esperaram a confirmação demais.
     */
    public static ServiceTimes serviceTimes(Collection<KitchenItem> items, int lateConfirmations) {
        List<KitchenItem> delivered = items.stream().filter(i -> i.delivered() != null).toList();
        List<Duration> passe = delivered.stream().filter(i -> i.ready() != null)
                .map(i -> positive(Duration.between(i.ready(), i.delivered()))).toList();
        List<Duration> toTable = delivered.stream()
                .map(i -> positive(Duration.between(i.received(), i.delivered()))).toList();
        return new ServiceTimes(passe.isEmpty() ? Optional.empty() : Optional.of(averageOf(passe)),
                toTable.isEmpty() ? Optional.empty() : Optional.of(averageOf(toTable)), lateConfirmations);
    }

    /** O preparo e o passe médios dos itens entregues, pela hora (no fuso) em que chegaram (REL-32). */
    public static List<HourTimes> byHour(Collection<KitchenItem> items, ZoneId zone) {
        return items.stream()
                .filter(i -> i.ready() != null && i.delivered() != null)
                .collect(Collectors.groupingBy(i -> i.received().atZone(zone).getHour(), TreeMap::new,
                        Collectors.toList()))
                .entrySet().stream()
                .map(e -> new HourTimes(e.getKey(),
                        averageOf(e.getValue().stream().map(i -> positive(Duration.between(i.received(), i.ready())))
                                .toList()),
                        averageOf(e.getValue().stream().map(i -> positive(Duration.between(i.ready(), i.delivered())))
                                .toList()),
                        e.getValue().size()))
                .toList();
    }

    /**
     * O máximo de itens em preparo ao mesmo tempo em cada hora de cada dia (REL-34): cada item entra ao
     * chegar e sai ao ficar pronto; num empate, o que sai vai antes do que entra.
     */
    public static List<HourPeak> peaks(Collection<KitchenItem> items, ZoneId zone) {
        record Event(Instant at, int delta) {
        }
        List<Event> events = new ArrayList<>();
        for (KitchenItem i : items) {
            if (i.ready() != null) {
                events.add(new Event(i.received(), 1));
                events.add(new Event(i.ready().isBefore(i.received()) ? i.received() : i.ready(), -1));
            }
        }
        events.sort(Comparator.comparing(Event::at).thenComparingInt(Event::delta));
        Map<LocalDate, Map<Integer, Integer>> peaks = new TreeMap<>();
        int preparing = 0;
        for (Event e : events) {
            preparing += e.delta();
            var local = e.at().atZone(zone);
            peaks.computeIfAbsent(local.toLocalDate(), d -> new TreeMap<>())
                    .merge(local.getHour(), preparing, Math::max);
        }
        List<HourPeak> result = new ArrayList<>();
        peaks.forEach((day, hours) -> hours.forEach((hour, peak) -> {
            if (peak > 0) {
                result.add(new HourPeak(day, hour, peak));
            }
        }));
        return result;
    }

    /** O preparo médio por categoria, da mais lenta para a mais rápida (REL-36). */
    public static List<CategoryPrep> byCategory(Collection<PrepTime> times) {
        return times.stream()
                .collect(Collectors.groupingBy(PrepTime::category))
                .entrySet().stream()
                .map(e -> new CategoryPrep(e.getKey(), e.getValue().size(),
                        averageOf(e.getValue().stream().map(PrepTime::time).toList())))
                .sorted(Comparator.comparing(CategoryPrep::average).reversed().thenComparing(CategoryPrep::category))
                .toList();
    }

    /**
     * As horas (de cada dia, no fuso, pela hora em que o item ficou pronto) com mais itens parados no
     * passe mais que {@code late}, com a espera média (REL-37). Empate: a espera maior, depois a primeira.
     */
    public static List<DelayedSlot> delayed(Collection<KitchenItem> items, ZoneId zone, Duration late, int limit) {
        record Slot(LocalDate day, int hour) {
        }
        return items.stream()
                .filter(i -> i.ready() != null && i.delivered() != null
                        && Duration.between(i.ready(), i.delivered()).compareTo(late) > 0)
                .collect(Collectors.groupingBy(i -> {
                    var local = i.ready().atZone(zone);
                    return new Slot(local.toLocalDate(), local.getHour());
                }))
                .entrySet().stream()
                .map(e -> new DelayedSlot(e.getKey().day(), e.getKey().hour(), e.getValue().size(),
                        averageOf(e.getValue().stream().map(i -> Duration.between(i.ready(), i.delivered())).toList())))
                .sorted(Comparator.comparingInt(DelayedSlot::items).reversed()
                        .thenComparing(Comparator.comparing(DelayedSlot::averageWait).reversed())
                        .thenComparing(DelayedSlot::day)
                        .thenComparingInt(DelayedSlot::hour))
                .limit(limit)
                .toList();
    }

    private static Duration positive(Duration duration) {
        return duration.isNegative() ? Duration.ZERO : duration;
    }
}
