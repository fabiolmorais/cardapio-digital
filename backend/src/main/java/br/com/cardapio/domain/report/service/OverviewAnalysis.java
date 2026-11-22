package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.ForecastDay;
import br.com.cardapio.domain.report.model.PrepAverage;
import br.com.cardapio.domain.report.model.TodayForecast;
import br.com.cardapio.domain.shared.model.Money;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** As regras dos blocos completos da Visão geral (REL-10, 33, 61, 23). */
public final class OverviewAnalysis {

    /** A previsão olha as últimas 4 semanas (o mesmo dia da semana). */
    public static final int FORECAST_WEEKS = 4;

    /** Um produto só é ponto de atenção com 3 cancelamentos ou mais no período. */
    static final int MIN_CANCELED = 3;

    /** Quantas mesas o ponto das comandas esquecidas cita pelo nome. */
    public static final int FORGOTTEN_NAMED = 3;

    private OverviewAnalysis() {
    }

    /**
     * A previsão de hoje pela média dos mesmos dias da semana anteriores que tiveram venda. Vazio sem
     * nenhum desses dias (restaurante novo, ou o dia em que a casa não abre).
     */
    public static Optional<TodayForecast> forecast(List<ForecastDay> past, Money soFar) {
        List<ForecastDay> open = past.stream().filter(d -> d.full().cents() > 0).toList();
        if (open.isEmpty()) {
            return Optional.empty();
        }
        long full = open.stream().mapToLong(d -> d.full().cents()).sum();
        long untilNow = open.stream().mapToLong(d -> d.untilNow().cents()).sum();
        return Optional.of(new TodayForecast(soFar, Money.ofCents(Math.round((double) full / open.size())),
                Money.ofCents(Math.round((double) untilNow / open.size())), open.size()));
    }

    /**
     * O produto mais cancelado (com {@link #MIN_CANCELED} cancelamentos ou mais; empate: o nome), com o
     * motivo mais comum (só se algum motivo apareceu mais de uma vez).
     */
    public static Optional<AttentionPoint.CanceledProduct> mostCanceled(Collection<CanceledProductCount> counts) {
        Map<String, List<CanceledProductCount>> byProduct = counts.stream()
                .collect(Collectors.groupingBy(CanceledProductCount::name));
        return byProduct.entrySet().stream()
                .map(e -> {
                    int times = e.getValue().stream().mapToInt(CanceledProductCount::times).sum();
                    Optional<CanceledProductCount> reason = e.getValue().stream()
                            .filter(c -> c.reason() != null && !c.reason().isBlank() && c.times() > 1)
                            .max(Comparator.comparingInt(CanceledProductCount::times)
                                    .thenComparing(CanceledProductCount::reason, Comparator.reverseOrder()));
                    return new AttentionPoint.CanceledProduct(e.getKey(), times,
                            reason.map(CanceledProductCount::reason).orElse(null),
                            reason.map(CanceledProductCount::times).orElse(0));
                })
                .filter(p -> p.times() >= MIN_CANCELED)
                .max(Comparator.comparingInt(AttentionPoint.CanceledProduct::times)
                        .thenComparing(AttentionPoint.CanceledProduct::name, Comparator.reverseOrder()));
    }

    /**
     * Os pontos de atenção, na ordem do desenho: o item que demorou, as comandas esquecidas e o mais
     * cancelado. Só os que existem.
     */
    public static List<AttentionPoint> attention(Collection<PrepAverage> period, Collection<PrepAverage> usual,
                                                 List<String> forgottenTables,
                                                 Collection<CanceledProductCount> canceled) {
        List<AttentionPoint> points = new ArrayList<>();
        DailySummaryAnalysis.attentionOf(period, usual).map(AttentionPoint.SlowItem::new).ifPresent(points::add);
        if (!forgottenTables.isEmpty()) {
            points.add(new AttentionPoint.ForgottenTabs(forgottenTables));
        }
        mostCanceled(canceled).ifPresent(points::add);
        return points;
    }
}
