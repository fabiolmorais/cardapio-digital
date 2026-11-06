package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.ResetPasswordCommand;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.PasswordResetPort;
import br.com.cardapio.application.port.out.email.EmailMessage;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.domain.account.exception.PasswordResetLinkInvalidException;
import br.com.cardapio.domain.account.model.PasswordHash;
import br.com.cardapio.domain.account.model.PasswordResetToken;
import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Email;
import br.com.cardapio.domain.shared.model.TokenHash;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private final User marina = User.create("Marina", new Email("marina@sakura.com"), new PasswordHash("hash:antiga"));
    private final User carlos = User.teamMember("Carlos"); // equipe de PIN: sem e-mail, sem senha

    /** O banco dos testes: os links e a senha de cada pessoa. */
    private final Map<TokenHash, PasswordResetToken> links = new HashMap<>();
    private final Map<UserId, PasswordHash> passwords = new HashMap<>();
    private final List<UserId> sessionsEnded = new ArrayList<>();
    private final List<EmailMessage> sent = new ArrayList<>();
    private int nextToken;

    private final LoadUserPort users = new LoadUserPort() {
        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return Optional.of(marina).filter(u -> u.email().equals(email));
        }

        @Override
        public Optional<User> findById(UserId id) {
            return Optional.empty();
        }
    };

    private final PasswordResetPort resets = new PasswordResetPort() {
        @Override
        public void save(PasswordResetToken token) {
            links.put(token.hash(), token);
        }

        @Override
        public Optional<PasswordResetToken> findByHash(TokenHash hash) {
            return Optional.ofNullable(links.get(hash));
        }

        @Override
        public long countSince(UserId userId, Instant since) {
            return links.values().stream().filter(t -> t.userId().equals(userId) && !t.createdAt().isBefore(since)).count();
        }

        @Override
        public void changePassword(UserId userId, PasswordHash newHash, Instant now) {
            passwords.put(userId, newHash);
            sessionsEnded.add(userId);
        }
    };

    private final PasswordHasher hasher = new PasswordHasher() {
        @Override
        public PasswordHash hash(String raw) {
            return new PasswordHash("hash:" + raw);
        }

        @Override
        public boolean matches(String raw, PasswordHash hash) {
            return hash.value().equals("hash:" + raw);
        }
    };

    private void requestAt(Instant at, String email) {
        new RequestPasswordResetService(users, resets, () -> "token-" + (++nextToken), sent::add,
                Clock.fixed(at, ZoneOffset.UTC), "https://app.test").request(email);
    }

    private void resetAt(Instant at, String token, String password) {
        new ResetPasswordService(resets, hasher, Clock.fixed(at, ZoneOffset.UTC))
                .reset(new ResetPasswordCommand(token, password));
    }

    @Test
    void theLinkGoesByEmailAndOnlyItsHashIsStored() {
        requestAt(NOW, "Marina@Sakura.com");

        assertThat(sent).singleElement().satisfies(m -> {
            assertThat(m.to().value()).isEqualTo("marina@sakura.com");
            assertThat(m.subject()).isEqualTo("Redefina sua senha");
            assertThat(m.text()).contains("https://app.test/redefinir-senha?token=token-1").contains("30 minutos");
        });
        assertThat(links).containsOnlyKeys(TokenHash.of("token-1"));
    }

    @Test
    void unknownEmailsAndThePinTeamGetNothingButTheSameAnswer() {
        requestAt(NOW, "ninguem@sakura.com");

        assertThat(sent).isEmpty();
        assertThat(links).isEmpty();
        assertThat(carlos.hasPasswordLogin()).isFalse(); // quem entra por PIN nem tem e-mail para receber
    }

    @Test
    void threeLinksPerHourAtMost() {
        for (int i = 0; i < 4; i++) {
            requestAt(NOW.plus(Duration.ofMinutes(i)), "marina@sakura.com");
        }
        assertThat(sent).hasSize(3);

        requestAt(NOW.plus(Duration.ofMinutes(61)), "marina@sakura.com");
        assertThat(sent).hasSize(4);
    }

    @Test
    void theLinkChangesThePasswordOnceAndEndsTheSessions() {
        requestAt(NOW, "marina@sakura.com");

        resetAt(NOW.plus(Duration.ofMinutes(10)), "token-1", "sushi2027novo");

        assertThat(passwords).containsEntry(marina.id(), new PasswordHash("hash:sushi2027novo"));
        assertThat(sessionsEnded).containsExactly(marina.id());
        assertThatThrownBy(() -> resetAt(NOW.plus(Duration.ofMinutes(11)), "token-1", "sushi2027outra"))
                .isInstanceOf(PasswordResetLinkInvalidException.class);
    }

    @Test
    void aWeakPasswordDoesNotBurnTheLinkAndAnInventedTokenIsRefused() {
        requestAt(NOW, "marina@sakura.com");

        assertThatThrownBy(() -> resetAt(NOW, "token-1", "curta")).isInstanceOf(InvalidValueException.class);
        resetAt(NOW, "token-1", "sushi2027novo"); // o link continua valendo
        assertThatThrownBy(() -> resetAt(NOW, "token-inventado", "sushi2027novo"))
                .isInstanceOf(PasswordResetLinkInvalidException.class);
        assertThatThrownBy(() -> resetAt(NOW.plus(Duration.ofMinutes(40)), "token-1", "sushi2027novo"))
                .isInstanceOf(PasswordResetLinkInvalidException.class);
    }
}
