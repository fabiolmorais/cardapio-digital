package br.com.cardapio.domain.shared.model;


import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.exception.OutdatedListException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayOrderTest {

    @Test
    void thePositionIsThePlaceInTheList() {
        DisplayOrder<String> order = new DisplayOrder<>(List.of("sushis", "entradas", "bebidas"));

        order.requireSameItemsAs(List.of("entradas", "sushis", "bebidas"));
        assertThat(order.positionOf("sushis")).isZero();
        assertThat(order.positionOf("bebidas")).isEqualTo(2);
    }

    @Test
    void repeatedItemsAreInvalid() {
        assertThatThrownBy(() -> new DisplayOrder<>(List.of("a", "b", "a")))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aMissingOrAnExtraItemMeansTheScreenIsOutdated() {
        DisplayOrder<String> order = new DisplayOrder<>(List.of("a", "b"));

        assertThatThrownBy(() -> order.requireSameItemsAs(List.of("a", "b", "c")))
                .isInstanceOf(OutdatedListException.class);
        assertThatThrownBy(() -> order.requireSameItemsAs(List.of("a")))
                .isInstanceOf(OutdatedListException.class);
        assertThatThrownBy(() -> order.requireSameItemsAs(List.of("a", "x")))
                .isInstanceOf(OutdatedListException.class);
    }

}