package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabPeopleTest {

    @Test
    void fromOneToFifty() {
        assertThat(new TabPeople(1).value()).isEqualTo(1);
        assertThat(new TabPeople(50).value()).isEqualTo(50);
        assertThatThrownBy(() -> new TabPeople(0)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new TabPeople(51)).isInstanceOf(InvalidValueException.class);
    }
}
