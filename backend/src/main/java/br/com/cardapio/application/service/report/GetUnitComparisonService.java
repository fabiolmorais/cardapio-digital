package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.GetUnitComparisonUseCase;
import br.com.cardapio.application.port.in.report.UnitComparison;
import br.com.cardapio.application.port.in.report.UnitComparisonQuery;
import br.com.cardapio.application.port.out.account.UnitPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.application.port.out.report.RatingReportPort;
import br.com.cardapio.application.port.out.report.ReportSummaryPort;
import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.application.port.out.report.TeamReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UnitMembership;
import br.com.cardapio.domain.report.model.CallTotals;
import br.com.cardapio.domain.report.model.RatingStats;
import br.com.cardapio.domain.report.model.ReportAccess;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.report.model.UnitPerformance;
import br.com.cardapio.domain.report.service.RatingAnalysis;
import br.com.cardapio.domain.report.service.SalesAnalysis;
import br.com.cardapio.domain.report.service.UnitComparisonAnalysis;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A comparação entre unidades (REL-80). Cada unidade é somada com as mesmas consultas dos outros
 * relatórios, no próprio fuso, e só depois de conferir que a pessoa é dona de todas as unidades da
 * empresa: ninguém vê números de uma unidade em que não manda.
 */
public class GetUnitComparisonService implements GetUnitComparisonUseCase {

    private final SubscriptionPort subscriptions;
    private final UnitPort units;
    private final SalesReportPort sales;
    private final SummarizedReads reads;
    private final TeamReportPort team;
    private final RatingReportPort ratings;
    private final Clock clock;

    public GetUnitComparisonService(SubscriptionPort subscriptions, UnitPort units, SalesReportPort sales,
                                    KitchenReportPort kitchen, TeamReportPort team, RatingReportPort ratings,
                                    ReportSummaryPort summary, Clock clock) {
        this.subscriptions = subscriptions;
        this.units = units;
        this.sales = sales;
        this.reads = new SummarizedReads(summary, sales, kitchen);
        this.team = team;
        this.ratings = ratings;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UnitComparison get(UnitComparisonQuery query) {
        EstablishmentId current = query.establishmentId();
        var plan = subscriptions.find(current)
                .orElseThrow(() -> new IllegalStateException("restaurante sem assinatura"))
                .plan();
        List<UnitMembership> all = units.unitsFor(units.organizationOf(current), query.userId());
        UnitComparisonAnalysis.require(plan, all);
        ReportAccess access = ReportAccess.of(plan);
        access.requireFull();

        UnitMembership here = all.stream().filter(u -> u.id().equals(current)).findFirst()
                .orElseThrow(() -> new IllegalStateException("a unidade de agora não está na empresa"));
        Instant now = clock.instant();
        ReportPeriod period = new ReportPeriod(query.from(), query.to(), here.zone());
        access.checkPeriod(period, LocalDate.ofInstant(now, here.zone()));
        SeriesGrouping grouping = UnitComparisonAnalysis.grouping(period);

        List<UnitPerformance> performances = all.stream()
                .map(unit -> performance(unit, query.from(), query.to(), now, grouping))
                .sorted(Comparator.comparing((UnitPerformance u) -> u.current().revenue()).reversed()
                        .thenComparing(UnitPerformance::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new UnitComparison(period, period.comparison(), grouping, performances,
                UnitComparisonAnalysis.network(performances));
    }

    /** Os números de uma unidade, com os dias no fuso dela. */
    private UnitPerformance performance(UnitMembership unit, LocalDate from, LocalDate to, Instant now,
                                        SeriesGrouping grouping) {
        EstablishmentId id = unit.id();
        ReportPeriod period = new ReportPeriod(from, to, unit.zone());
        ReportPeriod comparison = period.comparison();
        // Como nos outros relatórios: o período que termina hoje compara até a mesma hora.
        Instant comparisonEnd = period.to().equals(LocalDate.ofInstant(now, unit.zone()))
                ? comparison.end().minus(Duration.between(now, period.end()))
                : comparison.end();
        CallTotals calls = team.calls(id, period.start(), period.end(), Optional.empty());
        RatingStats stats = ratings.stats(id, period.start(), period.end(), period.zone(),
                RatingAnalysis.LUNCH_UNTIL, Optional.empty());
        return new UnitPerformance(id, unit.name(),
                sales.totals(id, period.start(), period.end(), Optional.empty()),
                sales.totals(id, comparison.start(), comparisonEnd, Optional.empty()),
                UnitComparisonAnalysis.prep(reads.prepAverages(id, period.start(), period.end(), period.zone(),
                        Optional.empty())),
                UnitComparisonAnalysis.response(calls.calls(), calls.response()),
                new RatingStats.StarTotal(stats.count(), stats.starSum()),
                UnitComparisonAnalysis.perDay(SalesAnalysis.series(period,
                        sales.revenueByDay(id, period.start(), period.end(), period.zone(), Optional.empty()),
                        grouping)));
    }
}
