package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UnitMembership;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.plan.model.PlanId;
import br.com.cardapio.domain.plan.model.SupportLevel;
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
import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitComparisonAnalysisTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private static Plan plan(PlanFeature... features) {
        return new Plan(new PlanId(UUID.randomUUID()), "plano", "Plano", Money.ofCents(14990), Money.ofCents(149900),
                Limit.unlimited(), Limit.unlimited(), Limit.unlimited(), Limit.unlimited(), 0, Set.of(features),
                SupportLevel.WHATSAPP, 1);
    }

    private static UnitMembership unit(String name, boolean owner) {
        return new UnitMembership(EstablishmentId.newId(), name, SAO_PAULO, owner);
    }

    private static final Plan ENTERPRISE = plan(PlanFeature.REPORTS, PlanFeature.MULTI_UNIT);

    @Test
    void onlyTheEnterpriseOwnerOfEveryUnitCompares() {
        List<UnitMembership> both = List.of(unit("Pinheiros", true), unit("Moema", true));
        assertThat(UnitComparisonAnalysis.allows(ENTERPRISE, both)).isTrue();
        assertThatCode(() -> UnitComparisonAnalysis.require(ENTERPRISE, both)).doesNotThrowAnyException();

        // Sem o recurso de várias unidades (Business), com uma unidade só, ou dono de só uma parte: não.
        assertThat(UnitComparisonAnalysis.allows(plan(PlanFeature.REPORTS), both)).isFalse();
        assertThat(UnitComparisonAnalysis.allows(ENTERPRISE, List.of(unit("Pinheiros", true)))).isFalse();
        List<UnitMembership> partly = List.of(unit("Pinheiros", true), unit("Moema", false));
        assertThat(UnitComparisonAnalysis.allows(ENTERPRISE, partly)).isFalse();
        assertThatThrownBy(() -> UnitComparisonAnalysis.require(ENTERPRISE, partly))
                .isInstanceOf(UnitComparisonUnavailableException.class);
    }

    @Test
    void theEvolutionGroupsByDayWeekOrMonth() {
        LocalDate from = LocalDate.parse("2026-01-01");
        assertThat(UnitComparisonAnalysis.grouping(new ReportPeriod(from, from.plusDays(13), SAO_PAULO)))
                .isEqualTo(SeriesGrouping.DAY);
        assertThat(UnitComparisonAnalysis.grouping(new ReportPeriod(from, from.plusDays(14), SAO_PAULO)))
                .isEqualTo(SeriesGrouping.WEEK);
        assertThat(UnitComparisonAnalysis.grouping(new ReportPeriod(from, from.plusDays(182), SAO_PAULO)))
                .isEqualTo(SeriesGrouping.WEEK);
        assertThat(UnitComparisonAnalysis.grouping(new ReportPeriod(from, from.plusDays(183), SAO_PAULO)))
                .isEqualTo(SeriesGrouping.MONTH);
    }

    @Test
    void eachPointIsAnAveragePerDay() {
        // Uma semana cortada (3 dias) e uma inteira: a média por dia não confunde corte com queda.
        List<UnitPoint> points = UnitComparisonAnalysis.perDay(List.of(
                new SeriesPoint(LocalDate.parse("2026-11-20"), LocalDate.parse("2026-11-22"), Money.ofCents(30000), 10),
                new SeriesPoint(LocalDate.parse("2026-11-23"), LocalDate.parse("2026-11-29"), Money.ofCents(70001), 7)));
        assertThat(points.get(0).revenuePerDay()).isEqualTo(Money.ofCents(10000));
        assertThat(points.get(0).tabsPerDay()).isEqualByComparingTo(new BigDecimal("3.3"));
        assertThat(points.get(1).revenuePerDay()).isEqualTo(Money.ofCents(10000));
        assertThat(points.get(1).tabsPerDay()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void thePrepAverageWeighsEachProduct() {
        TimeAverage prep = UnitComparisonAnalysis.prep(List.of(
                new PrepAverage(ProductId.newId(), "Guioza", 3, Duration.ofMinutes(10)),
                new PrepAverage(ProductId.newId(), "Temaki", 1, Duration.ofMinutes(30))));
        assertThat(prep.count()).isEqualTo(4);
        assertThat(prep.average()).contains(Duration.ofMinutes(15));
        assertThat(UnitComparisonAnalysis.prep(List.of())).isEqualTo(TimeAverage.NONE);
    }

    @Test
    void theNetworkAddsUpAndWeighsTheAverages() {
        UnitPerformance pinheiros = new UnitPerformance(EstablishmentId.newId(), "Pinheiros",
                new ReportTotals(Money.ofCents(15000), 2), new ReportTotals(Money.ofCents(6000), 1),
                new TimeAverage(2, Optional.of(Duration.ofMinutes(15))),
                new TimeAverage(2, Optional.of(Duration.ofSeconds(90))),
                new RatingStats.StarTotal(5, 23), List.of());
        UnitPerformance moema = new UnitPerformance(EstablishmentId.newId(), "Moema",
                new ReportTotals(Money.ofCents(30000), 1), ReportTotals.EMPTY,
                new TimeAverage(1, Optional.of(Duration.ofMinutes(30))), TimeAverage.NONE,
                new RatingStats.StarTotal(2, 7), List.of());

        NetworkTotals network = UnitComparisonAnalysis.network(List.of(pinheiros, moema));
        assertThat(network.current()).isEqualTo(new ReportTotals(Money.ofCents(45000), 3));
        assertThat(network.current().averageTicket()).isEqualTo(Money.ofCents(15000));
        assertThat(network.compared()).isEqualTo(new ReportTotals(Money.ofCents(6000), 1));
        assertThat(network.prep().average()).contains(Duration.ofMinutes(20));
        assertThat(network.callResponse().average()).contains(Duration.ofSeconds(90));
        assertThat(network.ratingAverage()).isEqualTo(4.3);
        // Uma unidade com menos de 5 avaliações não mostra a nota.
        assertThat(moema.ratingAverage()).isNull();
        assertThat(pinheiros.ratingAverage()).isEqualTo(4.6);
    }

    @Test
    void anEmptyAverageWeighsNothing() {
        assertThat(new TimeAverage(0, Optional.of(Duration.ofMinutes(5))).average()).isEmpty();
        assertThat(TimeAverage.combine(List.of(TimeAverage.NONE, TimeAverage.NONE))).isEqualTo(TimeAverage.NONE);
    }
}
