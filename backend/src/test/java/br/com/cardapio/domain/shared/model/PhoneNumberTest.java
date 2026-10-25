package br.com.cardapio.domain.shared.model;


import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void keepsOnlyDigitsAndDropsCountryCode() {
        assertThat(new PhoneNumber("(11) 98765-4321").value()).isEqualTo("11987654321");
        assertThat(new PhoneNumber("+55 11 3333-4444").value()).isEqualTo("1133334444");
    }

    @Test
    void rejectsNumberWithoutAreaCode() {
        assertThatThrownBy(() -> new PhoneNumber("98765-4321")).isInstanceOf(InvalidValueException.class);
    }

}