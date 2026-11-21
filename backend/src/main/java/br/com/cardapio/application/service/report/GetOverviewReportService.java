package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetOverviewReportUseCase;
import br.com.cardapio.application.port.in.report.OverviewReport;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.service.SalesAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** A Visão geral (14.1), com os blocos básicos: o Pro em diante. */
public class GetOverviewReportService implements GetOverviewReportUseCase {

    /** Quantos produtos no "Itens mais vendidos". */
    static final int TOP_ITEMS = 5;

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final SalesReportPort sales;
    private final Clock clock;

    public GetOverviewReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                    SectorPort sectors, SalesReportPort sales, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.sales = sales;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewReport get(ReportQuery query) {
        ReportScope scope = ReportScope.resolve(query, subscriptions, timeZones, sectors, clock);
        EstablishmentId id = query.establishmentId();
        var period = scope.period();
        var comparison = scope.comparison();
        return new OverviewReport(
                scope.access().level(), period, comparison,
                sales.totals(id, period.start(), period.end(), scope.sector()),
                sales.totals(id, comparison.start(), scope.comparisonEnd(), scope.sector()),
                SalesAnalysis.compareDays(period,
                        sales.revenueByDay(id, period.start(), period.end(), period.zone(), scope.sector()),
                        comparison,
                        sales.revenueByDay(id, comparison.start(), scope.comparisonEnd(), period.zone(), scope.sector())),
                sales.payments(id, period.start(), period.end(), scope.sector()),
                SalesAnalysis.weekdayHourAverages(period, sales.revenueByWeekdayAndHour(id, period, scope.sector())),
                sales.topItems(id, period.start(), period.end(), scope.sector(), TOP_ITEMS));
    }
}
