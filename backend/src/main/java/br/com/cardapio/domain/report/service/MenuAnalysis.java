package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.OptionSales;
import br.com.cardapio.domain.report.model.ProductPair;
import br.com.cardapio.domain.report.model.VariantSales;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** As regras do relatório de cardápio (REL-13, 14, 16 e, no Business, 15, 18, 19, 21, 23, 26). */
public final class MenuAnalysis {

    /**
     * "Vendem pouco" (REL-14) só olha produtos ativos há mais de 30 dias: um prato novo ainda não teve
     * tempo de vender e não é candidato a sair do cardápio.
     */
    public static final Duration SLOW_SELLER_MIN_AGE = Duration.ofDays(30);

    /** Quantos produtos aparecem em "Vendem pouco". */
    public static final int SLOW_SELLERS = 5;

    private MenuAnalysis() {
    }

    /** Até quando o produto precisa ter sido cadastrado para entrar em "Vendem pouco". */
    public static Instant slowSellerCutoff(Instant now) {
        return now.minus(SLOW_SELLER_MIN_AGE);
    }

    /** Quantos itens em "Mais cancelados", em "Tamanhos" e em "Pedidos juntos". */
    public static final int TOP_CANCELED = 3;
    public static final int TOP_SIZES = 3;
    public static final int TOP_PAIRS = 3;

    /** Quantas opções em "Opções e adicionais". */
    public static final int TOP_OPTIONS = 4;

    /** Dois produtos só são "pedidos juntos" em 3 comandas ou mais. */
    public static final int MIN_TOGETHER = 3;

    /** Os produtos mais cancelados (cada um com o motivo que se repetiu), do mais para o menos. */
    public static List<AttentionPoint.CanceledProduct> mostCanceled(Collection<CanceledProductCount> counts,
                                                                    int limit) {
        return OverviewAnalysis.canceledRanking(counts, 1).stream().limit(limit).toList();
    }

    /**
     * Os produtos com dois tamanhos vendidos ou mais, dos que mais venderam: cada um com os tamanhos,
     * do mais escolhido para o menos.
     */
    public static List<List<VariantSales>> sizes(Collection<VariantSales> sales, int limit) {
        Map<ProductId, List<VariantSales>> byProduct = sales.stream()
                .filter(v -> v.quantity() > 0)
                .collect(Collectors.groupingBy(VariantSales::productId));
        return byProduct.values().stream()
                .filter(list -> list.size() >= 2)
                .sorted(Comparator.comparingInt((List<VariantSales> list) -> list.stream()
                                .mapToInt(VariantSales::quantity).sum()).reversed()
                        .thenComparing(list -> list.getFirst().product()))
                .limit(limit)
                .map(list -> list.stream()
                        .sorted(Comparator.comparingInt(VariantSales::quantity).reversed()
                                .thenComparing(VariantSales::variant))
                        .toList())
                .toList();
    }

    /** As opções mais escolhidas (empate: a que mais somou, depois o nome). */
    public static List<OptionSales> topOptions(Collection<OptionSales> options, int limit) {
        return options.stream()
                .sorted(Comparator.comparingInt(OptionSales::times).reversed()
                        .thenComparing(Comparator.comparingLong((OptionSales o) -> o.charged().cents()).reversed())
                        .thenComparing(OptionSales::option))
                .limit(limit)
                .toList();
    }

    /** O que os adicionais pagos somaram no período. */
    public static Money paidOptions(Collection<OptionSales> options) {
        return Money.ofCents(options.stream().mapToLong(o -> o.charged().cents()).sum());
    }

    /**
     * Os pares mais pedidos juntos (em {@link #MIN_TOGETHER} comandas ou mais), do que mais aparece
     * junto; cada um lido a partir do menos pedido dos dois.
     */
    public static List<ProductPair> pairs(Collection<ProductPair> pairs, int limit) {
        return pairs.stream()
                .filter(p -> p.together() >= MIN_TOGETHER)
                .map(ProductPair::leading)
                .sorted(Comparator.comparingInt(ProductPair::together).reversed()
                        .thenComparing(Comparator.comparingDouble(ProductPair::share).reversed())
                        .thenComparing(ProductPair::first))
                .limit(limit)
                .toList();
    }
}
