package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetOverviewReportUseCase;
import br.com.cardapio.application.port.in.report.OverviewReport;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.in.report.OverviewFull;
import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.application.port.out.report.OperationReportPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.ForecastDay;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.TodayForecast;
import br.com.cardapio.domain.report.service.DailySummaryAnalysis;
import br.com.cardapio.domain.report.service.OverviewAnalysis;
import br.com.cardapio.domain.report.service.SalesAnalysis;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A Visão geral (14.1): os blocos básicos do Pro em diante; os completos, no Business. */
public class GetOverviewReportService implements GetOverviewReportUseCase {

    /** Quantos produtos no "Itens mais vendidos". */
    static final int TOP_ITEMS = 5;

    /** Quantos garçons no "Garçons da semana". */
    static final int WAITERS = 5;

    /** O "item que atrasou" dos pontos de atenção olha no máximo os últimos 31 dias do período. */
    static final Duration RECENT_ATTENTION = Duration.ofDays(31);

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final SectorPort sectors;
    private final SalesReportPort sales;
    private final OperationReportPort operations;
    private final KitchenReportPort kitchen;
    private final Clock clock;

    public GetOverviewReportService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                    SectorPort sectors, SalesReportPort sales, OperationReportPort operations,
                                    KitchenReportPort kitchen, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.sectors = sectors;
        this.sales = sales;
        this.operations = operations;
        this.kitchen = kitchen;
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
                sales.topItems(id, period.start(), period.end(), scope.sector(), TOP_ITEMS),
                scope.access().level().covers(ReportLevel.FULL) ? full(id, scope) : null);
    }

    /** Os blocos do Business. Quem só tem os básicos não recebe nada disto (o dado não sai do servidor). */
    private OverviewFull full(EstablishmentId id, ReportScope scope) {
        var period = scope.period();
        Optional<SectorId> sector = scope.sector();
        ZoneId zone = period.zone();
        Instant startOfToday = LocalDate.ofInstant(scope.now(), zone).atStartOfDay(zone).toInstant();
        // O item que atrasou olha o fim do período (no máximo 31 dias): ponto de atenção é o que está
        // acontecendo agora, e a consulta não cresce com 12 meses de histórico.
        Instant recentStart = period.start().isAfter(period.end().minus(RECENT_ATTENTION))
                ? period.start() : period.end().minus(RECENT_ATTENTION);
        Instant usualStart = recentStart.minus(Duration.ofDays(DailySummaryAnalysis.USUAL_DAYS));
        return new OverviewFull(
                operations.totals(id, period.start(), period.end(), sector),
                operations.totals(id, scope.comparison().start(), scope.comparisonEnd(), sector),
                today(id, scope),
                operations.waiters(id, period.start(), period.end(), sector, WAITERS),
                OverviewAnalysis.attention(
                        kitchen.averages(id, recentStart, period.end(), sector),
                        kitchen.averages(id, usualStart, recentStart, sector),
                        operations.forgottenTabs(id, startOfToday, sector),
                        operations.canceled(id, period.start(), period.end(), sector)));
    }

    /**
     * "Hoje, até agora": hoje, seja qual for o período escolhido, contra os mesmos dias da semana das
     * últimas 4 semanas, inteiros e até esta mesma hora.
     */
    private Optional<TodayForecast> today(EstablishmentId id, ReportScope scope) {
        ZoneId zone = scope.period().zone();
        Instant now = scope.now();
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Duration elapsed = Duration.between(start, now);
        List<ForecastDay> past = new ArrayList<>();
        for (int week = 1; week <= OverviewAnalysis.FORECAST_WEEKS; week++) {
            LocalDate day = today.minusWeeks(week);
            Instant dayStart = day.atStartOfDay(zone).toInstant();
            Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
            past.add(new ForecastDay(sales.totals(id, dayStart, dayEnd, scope.sector()).revenue(),
                    sales.totals(id, dayStart, dayStart.plus(elapsed), scope.sector()).revenue()));
        }
        Money soFar = sales.totals(id, start, now, scope.sector()).revenue();
        return OverviewAnalysis.forecast(past, soFar);
    }
}
