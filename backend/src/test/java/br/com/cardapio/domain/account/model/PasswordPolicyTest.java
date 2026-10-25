package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsLongPasswordWithLettersAndDigits() {
        assertThatCode(() -> PasswordPolicy.check("sushi2026bom")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"curta1", "somenteletras", "1234567890123"})
    void rejectsWeakPasswords(String password) {
        assertThatThrownBy(() -> PasswordPolicy.check(password)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void rejectsPasswordOverBcryptLimit() {
        assertThatThrownBy(() -> PasswordPolicy.check("a1".repeat(40))).isInstanceOf(InvalidValueException.class);
    }
}