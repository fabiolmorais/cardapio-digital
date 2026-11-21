package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetSalesReportUseCase;
import br.com.cardapio.application.port.in.report.ReportQuery;
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
import br.com.cardapio.domain.report.service.SalesAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/** Vendas (14.2), com os blocos básicos: o Pro em diante. */
public class GetSalesReportService implements GetSalesReportUseCase {

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
                SalesAnalysis.peakHour(byHour).orElse(null));
    }
}
