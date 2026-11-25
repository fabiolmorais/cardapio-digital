package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.application.port.out.report.ReportSummaryPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PrepAverage;
import br.com.cardapio.domain.report.model.PrepTotal;
import br.com.cardapio.domain.report.model.SummaryCoverage;
import br.com.cardapio.domain.report.model.TimeSpan;
import br.com.cardapio.domain.report.service.ReportSummaryPolicy;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * As leituras que o resumo diário atende (ADR 0014): os dias inteiros já resumidos vêm da tabela de
 * resumo; as pontas e os dias recentes, das consultas diretas. O resultado é o mesmo das consultas
 * diretas sobre o intervalo inteiro.
 */
final class SummarizedReads {

    private final ReportSummaryPort summary;
    private final SalesReportPort sales;
    private final KitchenReportPort kitchen;

    SummarizedReads(ReportSummaryPort summary, SalesReportPort sales, KitchenReportPort kitchen) {
        this.summary = summary;
        this.sales = sales;
        this.kitchen = kitchen;
    }

    private SummaryCoverage coverage(EstablishmentId id, Instant start, Instant end, ZoneId zone) {
        return ReportSummaryPolicy.coverage(start, end, zone, summary.summarizedUntil(id));
    }

    /** Os produtos mais vendidos (por faturamento, depois quantidade e nome), sem o rodízio. */
    List<ItemSales> topItems(EstablishmentId id, Instant start, Instant end, ZoneId zone, Optional<SectorId> sector,
                             int limit) {
        SummaryCoverage coverage = coverage(id, start, end, zone);
        if (!coverage.usesSummary()) {
            return sales.topItems(id, start, end, sector, limit);
        }
        Map<ProductId, ItemSales> byProduct = new LinkedHashMap<>();
        summary.sales(id, coverage.from().orElseThrow(), coverage.until().orElseThrow(), sector)
                .forEach(item -> byProduct.merge(item.productId(), item, SummarizedReads::plus));
        for (TimeSpan span : coverage.live()) {
            sales.topItems(id, span.start(), span.end(), sector, Integer.MAX_VALUE)
                    .forEach(item -> byProduct.merge(item.productId(), item, SummarizedReads::plus));
        }
        return byProduct.values().stream()
                .sorted(Comparator.comparing(ItemSales::revenue).reversed()
                        .thenComparing(Comparator.comparingInt(ItemSales::quantity).reversed())
                        .thenComparing(ItemSales::name))
                .limit(limit)
                .toList();
    }

    /** O preparo médio de cada produto (da chegada à cozinha até pronto). */
    List<PrepAverage> prepAverages(EstablishmentId id, Instant start, Instant end, ZoneId zone,
                                   Optional<SectorId> sector) {
        SummaryCoverage coverage = coverage(id, start, end, zone);
        if (!coverage.usesSummary()) {
            return kitchen.averages(id, start, end, sector);
        }
        Map<ProductId, PrepTotal> byProduct = new LinkedHashMap<>();
        summary.preparations(id, coverage.from().orElseThrow(), coverage.until().orElseThrow(), sector)
                .forEach(total -> byProduct.merge(total.productId(), total, PrepTotal::plus));
        for (TimeSpan span : coverage.live()) {
            kitchen.prepTotals(id, span.start(), span.end(), sector)
                    .forEach(total -> byProduct.merge(total.productId(), total, PrepTotal::plus));
        }
        return byProduct.values().stream().filter(t -> t.preparations() > 0).map(PrepTotal::average).toList();
    }

    private static ItemSales plus(ItemSales a, ItemSales b) {
        return new ItemSales(a.productId(), a.name(), a.quantity() + b.quantity(), a.revenue().plus(b.revenue()));
    }
}
