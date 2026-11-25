package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetTeamReportUseCase;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.in.report.TeamReport;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.RatingReportPort;
import br.com.cardapio.application.port.out.report.TeamReportPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.report.model.RatingComment;
import br.com.cardapio.domain.report.service.RatingAnalysis;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.TableCalls;
import br.com.cardapio.domain.report.service.TeamAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Atendimento e equipe (14.5): o relatório inteiro é do Business. */
public class GetTeamReportService implements GetTeamReportUseCase {

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final TeamReportPort team;
    private final RatingReportPort ratings;
    private final Clock clock;

    public GetTeamReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                SectorPort sectors, TeamReportPort team, RatingReportPort ratings, Clock clock) {
        this.ratings = ratings;
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.team = team;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamReport get(ReportQuery query) {
        ReportScope scope = ReportScope.resolve(query, subscriptions, timeZones, sectors, clock);
        scope.access().requireFull();
        EstablishmentId id = query.establishmentId();
        var period = scope.period();
        var sector = scope.sector();
        List<TableCalls> tables = team.tables(id, period.start(), period.end(), sector);
        return new TeamReport(period, scope.comparison(),
                team.calls(id, period.start(), period.end(), sector),
                team.calls(id, scope.comparison().start(), scope.comparisonEnd(), sector),
                team.waiters(id, period.start(), period.end(), sector),
                team.responseByHour(id, period.start(), period.end(), period.zone(), sector),
                team.byType(id, period.start(), period.end(), sector),
                TeamAnalysis.topTables(tables, TeamAnalysis.TOP_TABLES), TeamAnalysis.average(tables),
                team.duplicateWarnings(id, period.start(), period.end(), sector),
                TeamAnalysis.busiest(team.shiftLoads(id, period.start(), period.end(), period.zone(), sector),
                        TeamAnalysis.TOP_LOADS),
                ratings.stats(id, period.start(), period.end(), period.zone(), RatingAnalysis.LUNCH_UNTIL, sector),
                ratings.stats(id, scope.comparison().start(), scope.comparisonEnd(), period.zone(),
                        RatingAnalysis.LUNCH_UNTIL, sector).average(),
                RatingAnalysis.trend(period, ratings.byDay(id, period.start(), period.end(), period.zone(), sector)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<RatingComment> comments(ReportQuery query, boolean onlyLow, PageQuery page) {
        ReportScope scope = ReportScope.resolve(query, subscriptions, timeZones, sectors, clock);
        scope.access().requireFull();
        var period = scope.period();
        return ratings.comments(query.establishmentId(), period.start(), period.end(), scope.sector(), onlyLow, page);
    }
}
