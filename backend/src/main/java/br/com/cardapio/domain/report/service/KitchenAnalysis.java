package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.model.SlowItem;
import br.com.cardapio.domain.report.model.WeekdayHour;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
}
