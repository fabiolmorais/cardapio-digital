package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetSalesReportUseCase;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.in.report.SalesFull;
import br.com.cardapio.application.port.in.report.SalesReport;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.DailyRevenue;
import br.com.cardapio.domain.report.model.HourAverage;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CancelReasonTotal;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.service.SalesAnalysis;
import br.com.cardapio.domain.report.service.SalesFullAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Vendas (14.2), com os blocos básicos: o Pro em diante. */
public class GetSalesReportService implements GetSalesReportUseCase {

    /** Quantas mesas no "Mesas que mais vendem". */
    static final int TOP_TABLES = 6;

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final SalesReportPort sales;
    private final Clock clock;

    public GetSalesReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                 SectorPort sectors, SalesReportPort sales, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.sales = sales;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReport get(ReportQuery query, SeriesGrouping grouping) {
        Objects.requireNonNull(grouping, "agrupamento é obrigatório");
        ReportScope scope = ReportScope.resolve(query, subscriptions, timeZones, sectors, clock);
        EstablishmentId id = query.establishmentId();
        var period = scope.period();
        var comparison = scope.comparison();
        List<DailyRevenue> days = SalesAnalysis.everyDay(period,
                sales.revenueByDay(id, period.start(), period.end(), period.zone(), scope.sector()));
        List<HourAverage> byHour = SalesAnalysis.hourAverages(period,
                sales.revenueByWeekdayAndHour(id, period, scope.sector()));
        ReportTotals current = sales.totals(id, period.start(), period.end(), scope.sector());
        ReportTotals compared = sales.totals(id, comparison.start(), scope.comparisonEnd(), scope.sector());
        return new SalesReport(scope.access().level(), period, comparison, current, compared, grouping,
                SalesAnalysis.series(period, days, grouping), byHour,
                SalesAnalysis.bestDay(days).orElse(null),
                SalesAnalysis.tabsPerDay(period, current.tabs()),
                SalesAnalysis.tabsPerDay(comparison, compared.tabs()),
                SalesAnalysis.peakHour(byHour).orElse(null),
                scope.access().level().covers(ReportLevel.FULL) ? full(id, scope) : null);
    }

    /** Os blocos do Business. Quem só tem os básicos não recebe nada disto (o dado não sai do servidor). */
    private SalesFull full(EstablishmentId id, ReportScope scope) {
        var period = scope.period();
        var comparison = scope.comparison();
        Optional<SectorId> sector = scope.sector();
        Instant start = period.start();
        Instant end = period.end();
        Instant comparedStart = comparison.start();
        Instant comparedEnd = scope.comparisonEnd();
        List<CancelReasonTotal> reasons = sales.canceledByReason(id, start, end, sector);
        return new SalesFull(
                sales.bySector(id, start, end, sector), period.days(),
                sales.topTables(id, start, end, sector, TOP_TABLES),
                sales.rodizioTicket(id, start, end, sector),
                sales.rodizioTicket(id, comparedStart, comparedEnd, sector),
                sales.peopleTicket(id, start, end, sector),
                sales.peopleTicket(id, comparedStart, comparedEnd, sector),
                sales.advancePayments(id, start, end, sector),
                SalesFullAnalysis.total(reasons),
                SalesFullAnalysis.total(sales.canceledByReason(id, comparedStart, comparedEnd, sector)),
                SalesFullAnalysis.topReasons(reasons));
    }
}
