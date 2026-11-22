package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetTableReportUseCase;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.in.report.TableReport;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.TableReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.StayTotals;
import br.com.cardapio.domain.report.model.TableUsage;
import br.com.cardapio.domain.report.service.OccupancyAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Mesas e ocupação (14.6): o relatório inteiro é do Business. */
public class GetTableReportService implements GetTableReportUseCase {

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final TableReportPort report;
    private final Clock clock;

    public GetTableReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                 SectorPort sectors, TableReportPort report, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.report = report;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public TableReport get(ReportQuery query) {
        ReportScope scope = ReportScope.resolve(query, subscriptions, timeZones, sectors, clock);
        scope.access().requireFull();
        EstablishmentId id = query.establishmentId();
        var period = scope.period();
        var comparison = scope.comparison();
        var sector = scope.sector();
        List<TableUsage> tables = report.usage(id, period.start(), period.end(), sector);
        StayTotals current = report.stays(id, period.start(), period.end(), sector);
        StayTotals compared = report.stays(id, comparison.start(), scope.comparisonEnd(), sector);
        return new TableReport(period, comparison, current, compared,
                OccupancyAnalysis.turnover(current.tabs(), tables.size(), period.days()),
                OccupancyAnalysis.turnover(compared.tabs(), tables.size(), comparison.days()),
                OccupancyAnalysis.perOccupiedHour(current.revenue(), current.occupied()),
                OccupancyAnalysis.perOccupiedHour(compared.revenue(), compared.occupied()),
                tables,
                OccupancyAnalysis.occupancy(report.occupied(id, period.start(), period.end(), scope.now(), sector),
                        tables.size(), period.zone(), period.start(), period.end()),
                report.forgotten(id, period.start(), period.end(), scope.now(), OccupancyAnalysis.FORGOTTEN, sector,
                        OccupancyAnalysis.FORGOTTEN_SHOWN),
                report.moves(id, period.start(), period.end(), sector));
    }
}
