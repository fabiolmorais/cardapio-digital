package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginCommand;
import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.security.AccessToken;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.exception.InvalidCredentialsException;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.shared.model.Email;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginServiceTest {

    private final Fakes fakes = new Fakes();
    private final LoginService service = new LoginService(fakes, fakes, fakes, fakes);

    private User marina(boolean active) {
        User user = new User(UserId.newId(), "Marina", new Email("marina@sakura.com"),
                new PasswordHash("hash:sushi2026bom"), active);
        fakes.users.add(user);
        return user;
    }

    private ActiveMembership worksAt(User user, String establishmentName) {
        ActiveMembership m = new ActiveMembership(MembershipId.newId(), EstablishmentId.newId(), establishmentName,
                "sakura-sushi", Role.OWNER);
        fakes.memberships.add(new Fakes.Row(user.id(), m));
        return m;
    }

    @Test
    void issuesTokenForTheUserAndHerEstablishment() {
        User user = marina(true);
        ActiveMembership membership = worksAt(user, "Sakura Sushi");

        LoginResult result = service.login(new LoginCommand("MARINA@sakura.com", "sushi2026bom"));

        assertThat(result.userId()).isEqualTo(user.id());
        assertThat(result.establishment().establishmentId()).isEqualTo(membership.establishmentId());
        assertThat(result.accessToken()).isEqualTo("token-falso");
        assertThat(fakes.lastSubject).isEqualTo(new TokenSubject(user.id(), membership.establishmentId(), Role.OWNER));
    }

    @Test
    void sameErrorForWrongPasswordUnknownEmailOrInvalidEmail() {
        worksAt(marina(true), "Sakura Sushi");

        assertThatThrownBy(() -> service.login(new LoginCommand("marina@sakura.com", "errada12345")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("ninguem@sakura.com", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("nao-e-email", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void unknownEmailStillChecksAPassword() {
        int before = fakes.matchesCalls;
        assertThatThrownBy(() -> service.login(new LoginCommand("ninguem@sakura.com", "x")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(fakes.matchesCalls).isEqualTo(before + 1);
    }

    @Test
    void refusesInactiveUserOrUserWithoutEstablishment() {
        worksAt(marina(false), "Sakura Sushi");
        assertThatThrownBy(() -> service.login(new LoginCommand("marina@sakura.com", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);

        fakes.users.clear();
        fakes.memberships.clear();
        marina(true);
        assertThatThrownBy(() -> service.login(new LoginCommand("marina@sakura.com", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);
    }


    /** Todas as portas de saída em memória, numa classe só. */
    private static final class Fakes implements LoadUserPort, LoadMembershipPort, PasswordHasher, AccessTokenIssuer {
        record Row(UserId userId, ActiveMembership membership) {
        }

        final List<User> users = new ArrayList<>();
        final List<Row> memberships = new ArrayList<>();
        TokenSubject lastSubject;
        int matchesCalls;

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return users.stream().filter(u -> u.email().equals(email)).findFirst();
        }

        @Override
        public Optional<User> findById(UserId id) {
            return users.stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public List<ActiveMembership> findActiveByUser(UserId userId) {
            return memberships.stream().filter(r -> r.userId().equals(userId)).map(Row::membership).toList();
        }

        @Override
        public Optional<ActiveMembership> findActive(UserId userId, EstablishmentId establishmentId) {
            return findActiveByUser(userId).stream()
                    .filter(m -> m.establishmentId().equals(establishmentId)).findFirst();
        }

        @Override
        public PasswordHash hash(String rawPassword) {
            return new PasswordHash("hash:" + rawPassword);
        }

        @Override
        public boolean matches(String rawPassword, PasswordHash hash) {
            matchesCalls++;
            return hash.value().equals("hash:" + rawPassword);
        }

        @Override
        public AccessToken issue(TokenSubject subject) {
            lastSubject = subject;
            return new AccessToken("token-falso", Instant.parse("2026-10-25T20:00:00Z"));
        }
    }
}