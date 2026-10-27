package br.com.cardapio.domain.account.model;


import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"2580", "73915", "604172"})
    void acceptsFourToSixDigits(String pin) {
        assertThatCode(() -> PinPolicy.check(pin)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "258", "2580147", "25a0", " 2580", "２５８０"})
    void rejectsWrongFormat(String pin) {
        assertThatThrownBy(() -> PinPolicy.check(pin))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("de 4 a 6 números");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000", "77777", "1234", "456789", "4321", "9876"})
    void rejectsObviousPins(String pin) {
        assertThatThrownBy(() -> PinPolicy.check(pin))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("fácil de adivinhar");
    }
}