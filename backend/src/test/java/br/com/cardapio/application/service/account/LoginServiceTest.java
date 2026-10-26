package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginCommand;
import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.exception.InvalidCredentialsException;
import br.com.cardapio.domain.account.model.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginServiceTest {

    private final AccountFakes fakes = new AccountFakes();
    private final LoginService service = new LoginService(fakes, fakes, fakes, fakes.sessionIssuer());

    @Test
    void issuesTokensForTheUserAndHerEstablishment() {
        User user = fakes.addUser("Marina", true);
        ActiveMembership membership = fakes.addMembership(user, "Sakura Sushi");

        LoginResult result = service.login(new LoginCommand("MARINA@sakura.com", "sushi2026bom"));

        assertThat(result.userId()).isEqualTo(user.id());
        assertThat(result.establishment().establishmentId()).isEqualTo(membership.establishmentId());
        assertThat(result.accessToken()).isEqualTo("token-falso");
        assertThat(fakes.lastSubject).isEqualTo(new TokenSubject(user.id(), membership.establishmentId(), Role.OWNER));
    }

    @Test
    void storesOnlyTheHashOfTheRefreshToken() {
        fakes.addMembership(fakes.addUser("Marina", true), "Sakura Sushi");

        LoginResult result = service.login(new LoginCommand("marina@sakura.com", "sushi2026bom"));

        assertThat(fakes.refreshTokens).hasSize(1);
        assertThat(fakes.refreshTokens.values().iterator().next().hash())
                .isEqualTo(RefreshTokenHash.of(result.refreshToken()));
        assertThat(result.refreshTokenExpiresAt()).isAfter(result.expiresAt());
    }

    @Test
    void sameErrorForWrongPasswordUnknownEmailOrInvalidEmail() {
        fakes.addMembership(fakes.addUser("Marina", true), "Sakura Sushi");

        assertThatThrownBy(() -> service.login(new LoginCommand("marina@sakura.com", "errada12345")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("ninguem@sakura.com", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("nao-e-email", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(fakes.refreshTokens).isEmpty();
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
        fakes.addMembership(fakes.addUser("Marina", false), "Sakura Sushi");
        assertThatThrownBy(() -> service.login(new LoginCommand("marina@sakura.com", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);

        fakes.users.clear();
        fakes.memberships.clear();
        fakes.addUser("Marina", true);
        assertThatThrownBy(() -> service.login(new LoginCommand("marina@sakura.com", "sushi2026bom")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}