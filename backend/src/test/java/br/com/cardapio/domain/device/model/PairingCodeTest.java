package br.com.cardapio.domain.device.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PairingCodeTest {

    @ParameterizedTest
    @ValueSource(strings = {"482719", "004815", "000000"})
    void acceptsSixDigitsIncludingLeadingZeros(String code) {
        assertThat(new PairingCode(code).value()).isEqualTo(code);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "48271", "4827190", "48a719", " 482719"})
    void rejectsAnythingElse(String code) {
        assertThatThrownBy(() -> new PairingCode(code)).isInstanceOf(InvalidValueException.class);
    }
}
