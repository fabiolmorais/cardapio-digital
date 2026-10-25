package br.com.cardapio.domain.shared.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizesCaseAndSpaces() {
        assertThat(new Email("  Marina@SakuraSushi.com.BR ")
                .value()).isEqualTo("marina@sakurasushi.com.br");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> new Email("marina@")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(InvalidValueException.class);
    }
}