package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"258014", "739105", "604172", "091827"})
    void acceptsSixDigits(String pin) {
        assertThatCode(() -> PinPolicy.check(pin)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "2580", "25801", "2580147", "25a014", " 25801", "２５８０１４"})
    void rejectsAnythingButSixDigits(String pin) {
        assertThatThrownBy(() -> PinPolicy.check(pin))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("6 números");
    }

    @ParameterizedTest
    @ValueSource(strings = {"000000", "777777", "123456", "456789", "654321", "987654"})
    void rejectsObviousPins(String pin) {
        assertThatThrownBy(() -> PinPolicy.check(pin))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("fácil de adivinhar");
    }
}
