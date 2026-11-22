package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.ForecastDay;
import br.com.cardapio.domain.report.model.OperationTotals;
import br.com.cardapio.domain.report.model.PrepAverage;
import br.com.cardapio.domain.report.model.TodayForecast;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OverviewAnalysisTest {

    private static ForecastDay day(long full, long untilNow) {
        return new ForecastDay(Money.ofCents(full), Money.ofCents(untilNow));
    }

    @Test
    void theForecastAveragesOnlyTheWeeksWithSales() {
        var forecast = OverviewAnalysis.forecast(
                List.of(day(940000, 584000), day(0, 0), day(900000, 600000), day(980000, 568000)),
                Money.ofCents(612000));

        assertThat(forecast).contains(new TodayForecast(Money.ofCents(612000), Money.ofCents(940000),
                Money.ofCents(584000), 3));
        assertThat(OverviewAnalysis.forecast(List.of(day(0, 0)), Money.ofCents(100))).isEmpty();
        assertThat(OverviewAnalysis.forecast(List.of(), Money.ZERO)).isEmpty();
    }

    @Test
    void theMostCanceledNeedsThreeAndBringsTheRepeatedReason() {
        var counts = List.of(new CanceledProductCount("Temaki salmão", "Acabou o salmão", 7),
                new CanceledProductCount("Temaki salmão", "Cliente desistiu", 1),
                new CanceledProductCount("Temaki salmão", null, 1),
                new CanceledProductCount("Guioza", "Cliente desistiu", 2));

        assertThat(OverviewAnalysis.mostCanceled(counts))
                .contains(new AttentionPoint.CanceledProduct("Temaki salmão", 9, "Acabou o salmão", 7));
        // Dois cancelamentos não chegam a ser ponto de atenção.
        assertThat(OverviewAnalysis.mostCanceled(List.of(new CanceledProductCount("Guioza", "x", 2)))).isEmpty();
        // Sem motivo que se repita: só o número.
        assertThat(OverviewAnalysis.mostCanceled(List.of(new CanceledProductCount("Guioza", "a", 1),
                new CanceledProductCount("Guioza", "b", 1), new CanceledProductCount("Guioza", null, 1))))
                .contains(new AttentionPoint.CanceledProduct("Guioza", 3, null, 0));
    }

    @Test
    void theAttentionPointsComeInTheOrderOfTheDesignAndOnlyWhenTheyExist() {
        ProductId guioza = new ProductId(UUID.randomUUID());
        List<PrepAverage> slow = List.of(new PrepAverage(guioza, "Guioza", 3, Duration.ofMinutes(31)));
        List<PrepAverage> usual = List.of(new PrepAverage(guioza, "Guioza", 10, Duration.ofMinutes(14)));

        List<AttentionPoint> points = OverviewAnalysis.attention(slow, usual, List.of("07", "D3"),
                List.of(new CanceledProductCount("Temaki", "Acabou", 3)));

        assertThat(points).hasSize(3);
        assertThat(points.get(0)).isInstanceOf(AttentionPoint.SlowItem.class);
        assertThat(points.get(1)).isEqualTo(new AttentionPoint.ForgottenTabs(List.of("07", "D3")));
        assertThat(points.get(2)).isInstanceOf(AttentionPoint.CanceledProduct.class);
        assertThat(OverviewAnalysis.attention(List.of(), List.of(), List.of(), List.of())).isEmpty();
    }

    @Test
    void thePhoneShareHasOneDecimalAndIsEmptyWithoutOrders() {
        var totals = new OperationTotals(Optional.empty(), 2, 1, Optional.empty(), Optional.empty(), Money.ZERO);
        assertThat(totals.phoneShare()).contains(new BigDecimal("66.7"));
        assertThat(new OperationTotals(Optional.empty(), 0, 0, Optional.empty(), Optional.empty(), Money.ZERO)
                .phoneShare()).isEmpty();
    }
}
