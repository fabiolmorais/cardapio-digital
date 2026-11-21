package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.HourlyRevenue;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.model.SlowAttention;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DailySummaryAnalysisTest {

    private static final ProductId GUIOZA = new ProductId(UUID.randomUUID());
    private static final ProductId TEMAKI = new ProductId(UUID.randomUUID());

    private static List<PrepTime> preps(ProductId product, String name, int count, int minutes) {
        return IntStream.range(0, count)
                .mapToObj(i -> new PrepTime(product, name, "Pratos", 5, 20, Duration.ofMinutes(minutes))).toList();
    }

    @Test
    void thePeakIsTheHourWithTheMostRevenueAndTheEarlierOnATie() {
        var hours = List.of(new HourlyRevenue(5, 19, Money.ofCents(50000), 4),
                new HourlyRevenue(5, 21, Money.ofCents(90000), 6),
                new HourlyRevenue(5, 20, Money.ofCents(90000), 7));

        assertThat(DailySummaryAnalysis.peak(hours)).hasValueSatisfying(h -> assertThat(h.hour()).isEqualTo(20));
        assertThat(DailySummaryAnalysis.peak(List.of())).isEmpty();
    }

    @Test
    void theAttentionIsTheItemThatPassedTheUsualTheMost() {
        List<PrepTime> day = new ArrayList<>();
        day.addAll(preps(GUIOZA, "Guioza", 3, 26)); // normal 14: +12 min
        day.addAll(preps(TEMAKI, "Temaki", 4, 15)); // normal 10: +5 min
        List<PrepTime> usual = new ArrayList<>();
        usual.addAll(preps(GUIOZA, "Guioza", 10, 14));
        usual.addAll(preps(TEMAKI, "Temaki", 10, 10));

        assertThat(DailySummaryAnalysis.attention(day, usual))
                .contains(new SlowAttention("Guioza", Duration.ofMinutes(26), Duration.ofMinutes(14)));
    }

    @Test
    void aSmallDelayOrTooFewPreparationsIsNoAttention() {
        // 20% acima do normal: não chega aos 25%.
        assertThat(DailySummaryAnalysis.attention(preps(GUIOZA, "Guioza", 5, 18), preps(GUIOZA, "Guioza", 10, 15)))
                .isEmpty();
        // 50% acima, mas só 2 min a mais.
        assertThat(DailySummaryAnalysis.attention(preps(GUIOZA, "Guioza", 5, 6), preps(GUIOZA, "Guioza", 10, 4)))
                .isEmpty();
        // Só 2 preparos no dia.
        assertThat(DailySummaryAnalysis.attention(preps(GUIOZA, "Guioza", 2, 40), preps(GUIOZA, "Guioza", 10, 14)))
                .isEmpty();
        // Sem um normal com 5 preparos (um prato novo).
        assertThat(DailySummaryAnalysis.attention(preps(GUIOZA, "Guioza", 5, 40), preps(GUIOZA, "Guioza", 4, 14)))
                .isEmpty();
    }
}
