package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabPeriodTest {

    private static final LocalDate DAY = LocalDate.parse("2026-11-03");

    @Test
    void oneDayOrUpToAYear() {
        assertThat(new TabPeriod(DAY, DAY).from()).isEqualTo(DAY);
        assertThat(new TabPeriod(DAY.minusDays(365), DAY).to()).isEqualTo(DAY);
    }

    @Test
    void notBackwardsNorLongerThanAYear() {
        assertThatThrownBy(() -> new TabPeriod(DAY, DAY.minusDays(1)))
                .isInstanceOf(InvalidValueException.class).hasMessage("A data final vem depois da inicial.");
        assertThatThrownBy(() -> new TabPeriod(DAY.minusDays(366), DAY))
                .isInstanceOf(InvalidValueException.class).hasMessage("Escolha um período de até um ano.");
    }
}
