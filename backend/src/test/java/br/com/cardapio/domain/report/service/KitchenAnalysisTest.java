package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.HourPeak;
import br.com.cardapio.domain.report.model.PeakAverage;
import br.com.cardapio.domain.report.model.PrepTime;
import br.com.cardapio.domain.report.model.SlowItem;
import br.com.cardapio.domain.report.model.WeekdayHour;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitchenAnalysisTest {

    private static final ProductId YAKISSOBA = new ProductId(UUID.randomUUID());
    private static final ProductId GUIOZA = new ProductId(UUID.randomUUID());
    private static final ProductId CHA = new ProductId(UUID.randomUUID());

    private static Duration min(int minutes) {
        return Duration.ofMinutes(minutes);
    }

    private static PrepTime prep(ProductId product, String name, int weekday, int hour, int minutes) {
        return new PrepTime(product, name, "Pratos", weekday, hour, min(minutes));
    }

    /** Preparos do item, todos no sábado às 20h. */
    private static List<PrepTime> preps(ProductId product, String name, int... minutes) {
        return IntStream.of(minutes).mapToObj(m -> prep(product, name, 6, 20, m)).toList();
    }

    @Test
    void nineInTenIsTheTimeThatNinetyPercentDidNotPass() {
        // 10 preparos: o 9º da fila (o esquecido de 60 min fica de fora).
        assertThat(KitchenAnalysis.ninetyPercent(List.of(min(10), min(12), min(9), min(11), min(14), min(13),
                min(15), min(16), min(18), min(60)))).isEqualTo(min(18));
        // 20 preparos: o 18º.
        List<Duration> twenty = IntStream.rangeClosed(1, 20).mapToObj(KitchenAnalysisTest::min).toList();
        assertThat(KitchenAnalysis.ninetyPercent(twenty)).isEqualTo(min(18));
        // Poucos preparos: o 90% cai no último.
        assertThat(KitchenAnalysis.ninetyPercent(List.of(min(5), min(7), min(6)))).isEqualTo(min(7));
        assertThat(KitchenAnalysis.ninetyPercent(List.of(min(4)))).isEqualTo(min(4));
        assertThatThrownBy(() -> KitchenAnalysis.ninetyPercent(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theAverageIsRoundedToTheSecondAndEmptyWithoutPreparations() {
        assertThat(KitchenAnalysis.average(List.of())).isEmpty();
        var times = List.of(new PrepTime(CHA, "Chá", "Bebidas", 1, 12, Duration.ofSeconds(100)),
                new PrepTime(CHA, "Chá", "Bebidas", 1, 12, Duration.ofSeconds(101)));
        assertThat(KitchenAnalysis.average(times)).contains(Duration.ofSeconds(101)); // 100,5 s
    }

    @Test
    void theSlowestNeedFivePreparationsAndGoByNineInTen() {
        List<PrepTime> times = new ArrayList<>();
        times.addAll(preps(YAKISSOBA, "Yakissoba", 15, 17, 19, 21, 28));
        // A média do Guioza é menor, mas o "9 em 10 até" é maior: ele vem primeiro.
        times.addAll(preps(GUIOZA, "Guioza", 8, 9, 10, 11, 31));
        // O chá demorou muito, mas só 4 vezes: fica de fora.
        times.addAll(preps(CHA, "Chá", 40, 40, 40, 40));

        List<SlowItem> slowest = KitchenAnalysis.slowest(times, 5);

        assertThat(slowest).extracting(SlowItem::name).containsExactly("Guioza", "Yakissoba");
        SlowItem guioza = slowest.getFirst();
        assertThat(guioza.preparations()).isEqualTo(5);
        assertThat(guioza.average()).isEqualTo(Duration.ofSeconds(828)); // 13,8 min
        assertThat(guioza.ninetyPercent()).isEqualTo(min(31));
        assertThat(guioza.category()).isEqualTo("Pratos");
        assertThat(KitchenAnalysis.slowest(times, 1)).extracting(SlowItem::name).containsExactly("Guioza");
    }

    @Test
    void aTieGoesToTheHigherAverageAndThenToTheName() {
        List<PrepTime> times = new ArrayList<>();
        times.addAll(preps(YAKISSOBA, "Yakissoba", 10, 10, 10, 10, 20));
        times.addAll(preps(GUIOZA, "Guioza", 15, 15, 15, 15, 20));
        times.addAll(preps(CHA, "Abobrinha", 15, 15, 15, 15, 20));

        assertThat(KitchenAnalysis.slowest(times, 5)).extracting(SlowItem::name)
                .containsExactly("Abobrinha", "Guioza", "Yakissoba");
    }

    @Test
    void theWorstHourIsTheSlowestOneWithEnoughPreparations() {
        List<PrepTime> times = List.of(
                // Sábado às 20h: 3 preparos, média 20 min.
                prep(YAKISSOBA, "Yakissoba", 6, 20, 18), prep(YAKISSOBA, "Yakissoba", 6, 20, 20),
                prep(YAKISSOBA, "Yakissoba", 6, 20, 22),
                // Sexta às 21h: 3 preparos, média 15 min.
                prep(YAKISSOBA, "Yakissoba", 5, 21, 15), prep(YAKISSOBA, "Yakissoba", 5, 21, 15),
                prep(YAKISSOBA, "Yakissoba", 5, 21, 15),
                // Domingo às 13h: o mais lento, mas só 2 preparos.
                prep(YAKISSOBA, "Yakissoba", 7, 13, 40), prep(YAKISSOBA, "Yakissoba", 7, 13, 40));

        assertThat(KitchenAnalysis.worstHour(times)).contains(new WeekdayHour(6, 20));
        assertThat(KitchenAnalysis.slowest(times, 5).getFirst().worstHour()).contains(new WeekdayHour(6, 20));
    }

    @Test
    void withoutAnHourWithEnoughPreparationsThereIsNoWorstHour() {
        List<PrepTime> times = List.of(prep(GUIOZA, "Guioza", 1, 12, 10), prep(GUIOZA, "Guioza", 2, 12, 10),
                prep(GUIOZA, "Guioza", 3, 12, 10), prep(GUIOZA, "Guioza", 4, 12, 10), prep(GUIOZA, "Guioza", 5, 12, 10));

        assertThat(KitchenAnalysis.worstHour(times)).isEmpty();
        assertThat(KitchenAnalysis.slowest(times, 5).getFirst().worstHour()).isEmpty();
    }

    @Test
    void aTieInTheWorstHourGoesToTheOneWithMorePreparations() {
        List<PrepTime> times = List.of(
                prep(GUIOZA, "Guioza", 1, 12, 10), prep(GUIOZA, "Guioza", 1, 12, 10), prep(GUIOZA, "Guioza", 1, 12, 10),
                prep(GUIOZA, "Guioza", 2, 19, 10), prep(GUIOZA, "Guioza", 2, 19, 10), prep(GUIOZA, "Guioza", 2, 19, 10),
                prep(GUIOZA, "Guioza", 2, 19, 10));

        assertThat(KitchenAnalysis.worstHour(times)).contains(new WeekdayHour(2, 19));
    }

    @Test
    void aPrepTimeIsNeverNegativeNorOutOfTheWeek() {
        assertThatThrownBy(() -> new PrepTime(CHA, "Chá", "Bebidas", 1, 12, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PrepTime(CHA, "Chá", "Bebidas", 8, 12, min(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PrepTime(CHA, "Chá", "Bebidas", 1, 24, min(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theAveragePeakOfEachHourIsOverTheDaysTheKitchenWorkedIt() {
        var monday = java.time.LocalDate.of(2026, 11, 16);
        var peaks = KitchenAnalysis.peakAverages(List.of(new HourPeak(monday, 21, 23), new HourPeak(monday.plusDays(1), 21, 20),
                new HourPeak(monday, 12, 4), new HourPeak(monday.plusDays(2), 21, 18)));

        assertThat(peaks).containsExactly(new PeakAverage(12, new java.math.BigDecimal("4.0")),
                new PeakAverage(21, new java.math.BigDecimal("20.3")));
        assertThat(KitchenAnalysis.peakAverages(List.of())).isEmpty();
    }
}
