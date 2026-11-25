package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.account.model.UnitMembership;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.report.exception.UnitComparisonUnavailableException;
import br.com.cardapio.domain.report.model.NetworkTotals;
import br.com.cardapio.domain.report.model.PrepAverage;
import br.com.cardapio.domain.report.model.RatingStats;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.report.model.SeriesPoint;
import br.com.cardapio.domain.report.model.TimeAverage;
import br.com.cardapio.domain.report.model.UnitPerformance;
import br.com.cardapio.domain.report.model.UnitPoint;
import br.com.cardapio.domain.shared.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/** A comparação entre unidades (REL-80): quem pode ver e as contas da rede. */
public final class UnitComparisonAnalysis {

    /** Com uma unidade só, não há o que comparar. */
    public static final int MIN_UNITS = 2;

    /** Até quantos dias a evolução mostra um ponto por dia. */
    public static final int DAILY_UP_TO = 14;

    /** Até quantos dias, um ponto por semana; acima, por mês. */
    public static final int WEEKLY_UP_TO = 183;

    private UnitComparisonAnalysis() {
    }

    /**
     * A comparação existe para a pessoa: o plano tem várias unidades (Enterprise), a empresa tem duas
     * unidades ativas ou mais e ela é dona de todas. Quem é dono de só uma parte não vê os números
     * das outras, nem somados.
     */
    public static boolean allows(Plan plan, List<UnitMembership> units) {
        return plan.includes(PlanFeature.MULTI_UNIT)
                && units.size() >= MIN_UNITS
                && units.stream().allMatch(UnitMembership::owner);
    }

    /** Sem a comparação: 404, como um recurso que não existe. */
    public static void require(Plan plan, List<UnitMembership> units) {
        if (!allows(plan, units)) {
            throw new UnitComparisonUnavailableException();
        }
    }

    /** Um ponto por dia até 14 dias, por semana até 6 meses e por mês acima disso. */
    public static SeriesGrouping grouping(ReportPeriod period) {
        int days = period.days();
        return days <= DAILY_UP_TO ? SeriesGrouping.DAY
                : days <= WEEKLY_UP_TO ? SeriesGrouping.WEEK
                : SeriesGrouping.MONTH;
    }

    /** Cada ponto vira a média por dia dos dias que ele cobre. */
    public static List<UnitPoint> perDay(List<SeriesPoint> points) {
        return points.stream().map(p -> {
            long days = ChronoUnit.DAYS.between(p.start(), p.end()) + 1;
            return new UnitPoint(p.start(), p.end(),
                    Money.ofCents(Math.round((double) p.revenue().cents() / days)),
                    BigDecimal.valueOf(p.tabs()).divide(BigDecimal.valueOf(days), 1, RoundingMode.HALF_UP));
        }).toList();
    }

    /** O preparo médio da unidade a partir das médias por produto, pesadas pelos preparos. */
    public static TimeAverage prep(List<PrepAverage> products) {
        return TimeAverage.combine(products.stream()
                .map(p -> new TimeAverage(p.preparations(), Optional.of(p.average())))
                .toList());
    }

    /** A resposta média ao chamado, pesada pelos chamados. */
    public static TimeAverage response(int calls, Optional<Duration> average) {
        return new TimeAverage(calls, average);
    }

    /** A rede: somas das unidades e médias pesadas (o tíquete sai da soma, não da média dos tíquetes). */
    public static NetworkTotals network(List<UnitPerformance> units) {
        Money revenue = Money.ZERO;
        Money comparedRevenue = Money.ZERO;
        int tabs = 0;
        int comparedTabs = 0;
        int ratings = 0;
        long stars = 0;
        for (UnitPerformance unit : units) {
            revenue = revenue.plus(unit.current().revenue());
            comparedRevenue = comparedRevenue.plus(unit.compared().revenue());
            tabs += unit.current().tabs();
            comparedTabs += unit.compared().tabs();
            ratings += unit.ratings().count();
            stars += unit.ratings().starSum();
        }
        return new NetworkTotals(new ReportTotals(revenue, tabs), new ReportTotals(comparedRevenue, comparedTabs),
                TimeAverage.combine(units.stream().map(UnitPerformance::prep).toList()),
                TimeAverage.combine(units.stream().map(UnitPerformance::callResponse).toList()),
                new RatingStats.StarTotal(ratings, stars));
    }
}
