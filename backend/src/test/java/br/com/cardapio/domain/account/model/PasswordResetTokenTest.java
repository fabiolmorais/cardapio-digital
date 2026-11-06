package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.account.exception.PasswordResetLinkInvalidException;
import br.com.cardapio.domain.shared.model.TokenHash;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private final PasswordResetToken link = PasswordResetToken.issue(UserId.newId(), TokenHash.of("abc"), NOW);

    @Test
    void theLinkLastsThirtyMinutes() {
        assertThat(link.expiresAt()).isEqualTo(Instant.parse("2026-11-06T12:30:00Z"));
        assertThat(link.usable(NOW.plus(Duration.ofMinutes(29)))).isTrue();
        assertThat(link.usable(NOW.plus(Duration.ofMinutes(30)))).isFalse();
        assertThatThrownBy(() -> link.use(NOW.plus(Duration.ofMinutes(31))))
                .isInstanceOf(PasswordResetLinkInvalidException.class);
    }

    @Test
    void itWorksOnlyOnce() {
        PasswordResetToken used = link.use(NOW.plusSeconds(60));

        assertThat(used.usedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThatThrownBy(() -> used.use(NOW.plusSeconds(61)))
                .isInstanceOf(PasswordResetLinkInvalidException.class)
                .hasMessage("Este link para redefinir a senha expirou ou já foi usado. Peça outro.");
    }
}
