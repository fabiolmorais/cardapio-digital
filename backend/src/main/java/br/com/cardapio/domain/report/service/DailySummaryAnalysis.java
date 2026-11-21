package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.HourlyRevenue;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.model.SlowAttention;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** As regras do resumo diário (REL-76). */
public final class DailySummaryAnalysis {

    /** O "normal" de um item é o preparo médio dele nos 28 dias antes do dia do resumo. */
    public static final int USUAL_DAYS = 28;

    /** No dia, o item precisa de 3 preparos; no normal, de 5. */
    static final int MIN_DAY = 3;
    static final int MIN_USUAL = 5;

    /** Só é ponto de atenção se passou do normal em 25% ou mais, e em 3 min ou mais. */
    static final double MIN_RATIO = 1.25;
    static final Duration MIN_EXTRA = Duration.ofMinutes(3);

    private DailySummaryAnalysis() {
    }

    /** A hora em que as mesas que mais faturaram chegaram (empate: a mais cedo). */
    public static Optional<HourlyRevenue> peak(Collection<HourlyRevenue> hours) {
        return hours.stream()
                .filter(h -> h.revenue().cents() > 0)
                .max(Comparator.comparingLong((HourlyRevenue h) -> h.revenue().cents())
                        .thenComparing(HourlyRevenue::hour, Comparator.reverseOrder()));
    }

    /**
     * O item que mais passou do normal no dia, se algum passou bastante: o preparo médio do dia (com
     * {@link #MIN_DAY} preparos ou mais) contra o dos {@link #USUAL_DAYS} dias antes (com
     * {@link #MIN_USUAL} ou mais), por pelo menos 25% e 3 min. Empate: o que passou mais minutos.
     */
    public static Optional<SlowAttention> attention(Collection<PrepTime> day, Collection<PrepTime> usual) {
        Map<ProductId, List<PrepTime>> usualByProduct = usual.stream()
                .collect(Collectors.groupingBy(PrepTime::productId));
        return day.stream()
                .collect(Collectors.groupingBy(PrepTime::productId))
                .entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_DAY)
                .filter(e -> usualByProduct.getOrDefault(e.getKey(), List.of()).size() >= MIN_USUAL)
                .map(e -> new SlowAttention(e.getValue().getFirst().name(),
                        KitchenAnalysis.average(e.getValue()).orElseThrow(),
                        KitchenAnalysis.average(usualByProduct.get(e.getKey())).orElseThrow()))
                .filter(a -> a.dayAverage().toMillis() >= a.usualAverage().toMillis() * MIN_RATIO)
                .filter(a -> a.dayAverage().minus(a.usualAverage()).compareTo(MIN_EXTRA) >= 0)
                .max(Comparator.comparing((SlowAttention a) -> a.dayAverage().minus(a.usualAverage()))
                        .thenComparing(SlowAttention::name, Comparator.reverseOrder()));
    }
}
