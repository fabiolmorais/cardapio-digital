package br.com.cardapio.domain.shared.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void rejectsNegativeValues() {
        assertThatThrownBy(() -> Money.ofCents(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addsValues() {
        assertThat(Money.ofCents(6990).plus(Money.ofCents(10))).isEqualTo(Money.ofCents(7000));
    }

    @Test
    void multipliesByQuantity() {
        assertThat(Money.ofCents(1250).times(3)).isEqualTo(Money.ofCents(3750));
        assertThat(Money.ofCents(1250).times(0)).isEqualTo(Money.ZERO);
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> Money.ofCents(100).times(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failsOnOverflowInsteadOfWrapping() {
        assertThatThrownBy(() -> Money.ofCents(Long.MAX_VALUE).plus(Money.ofCents(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void comparesByValue() {
        assertThat(Money.ofCents(100)).isLessThan(Money.ofCents(200));
    }
}
