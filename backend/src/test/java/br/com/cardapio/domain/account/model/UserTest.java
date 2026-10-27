package br.com.cardapio.domain.account.model;


import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Email;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void teamMemberHasNoEmailNorPassword() {
        User carlos = User.teamMember("  Carlos  ");

        assertThat(carlos.name()).isEqualTo("Carlos");
        assertThat(carlos.email()).isNull();
        assertThat(carlos.hasPasswordLogin()).isFalse();
    }

    @Test
    void teamMemberNeedsAName() {
        assertThatThrownBy(() -> User.teamMember(" ")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void emailAndPasswordGoTogether() {
        assertThatThrownBy(() -> new User(UserId.newId(), "Ana", new Email("ana@sakura.com"), null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User(UserId.newId(), "Ana", null, new PasswordHash("hash"), true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}