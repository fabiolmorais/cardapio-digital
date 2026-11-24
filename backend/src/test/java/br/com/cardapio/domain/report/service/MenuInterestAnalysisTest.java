package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.CartSessions;
import br.com.cardapio.domain.report.model.LeftInCart;
import br.com.cardapio.domain.report.model.MenuInterest;
import br.com.cardapio.domain.report.model.ProductInterest;
import br.com.cardapio.domain.report.model.ProductInterestRow;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Visto x pedido, reels e carrinhos (REL-27, 28, 29). */
class MenuInterestAnalysisTest {

    private static final Instant SINCE = Instant.parse("2026-11-24T23:00:00Z");
    private static final CartSessions CARTS = new CartSessions(4, 1);

    private static ProductInterestRow row(String name, long price, int viewers, int ordered, int reel, int reelOrdered,
                                          int leftSessions, int leftUnits) {
        return new ProductInterestRow(name, Money.ofCents(price), viewers, ordered, reel, reelOrdered, leftSessions,
                leftSessions, leftUnits);
    }

    @Test
    void theMostViewedComeFirstWithTheConversion() {
        MenuInterest result = MenuInterestAnalysis.of(SINCE, List.of(
                row("Chá", 900, 2, 2, 0, 0, 0, 0),
                row("Guioza", 2900, 8, 2, 5, 2, 0, 0),
                row("Gelo", 0, 0, 0, 0, 0, 1, 1)), CARTS);

        assertThat(result.viewed()).containsExactly(new ProductInterest("Guioza", 8, 2),
                new ProductInterest("Chá", 2, 2));
        assertThat(result.viewed().getFirst().conversion()).isEqualTo(0.25);
        assertThat(result.reels()).containsExactly(new ProductInterest("Guioza", 5, 2));
        assertThat(new ProductInterest("Nada", 0, 0).conversion()).isZero();
        assertThat(result.recordingSince()).isEqualTo(SINCE);
        assertThat(result.carts()).isEqualTo(CARTS);
    }

    @Test
    void whatWasLeftInTheCartIsWorthTodaysPrice() {
        MenuInterest result = MenuInterestAnalysis.of(SINCE, List.of(
                row("Chá", 900, 0, 0, 0, 0, 1, 3),
                row("Lámen", 3500, 0, 0, 0, 0, 2, 2),
                row("Guioza", 2900, 0, 0, 0, 0, 0, 0)), CARTS);

        assertThat(result.leftValue()).isEqualTo(Money.ofCents(3 * 900 + 2 * 3500));
        assertThat(result.mostLeft()).containsExactly(new LeftInCart("Lámen", 2, 2), new LeftInCart("Chá", 1, 3));
    }

    @Test
    void theListsHaveALimit() {
        List<ProductInterestRow> many = IntStream.range(0, 30)
                .mapToObj(i -> row("P" + (100 + i), 100, i + 1, 0, i + 1, 0, i + 1, 1))
                .toList();

        MenuInterest result = MenuInterestAnalysis.of(null, many, CARTS);

        assertThat(result.viewed()).hasSize(MenuInterestAnalysis.TOP_VIEWED);
        assertThat(result.viewed().getFirst().name()).isEqualTo("P129");
        assertThat(result.reels()).hasSize(MenuInterestAnalysis.TOP_REELS);
        assertThat(result.mostLeft()).hasSize(MenuInterestAnalysis.TOP_LEFT);
        assertThat(result.recordingSince()).isNull();
    }
}
