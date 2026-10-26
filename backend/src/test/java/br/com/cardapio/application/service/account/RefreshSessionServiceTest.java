package br.com.cardapio.application.service.account;


import br.com.cardapio.application.port.in.account.LoginCommand;
import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.domain.account.exception.InvalidSessionException;
import br.com.cardapio.domain.account.model.RefreshToken;
import br.com.cardapio.domain.account.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshSessionServiceTest {

    private final AccountFakes fakes = new AccountFakes();
    private final SessionIssuer sessions = fakes.sessionIssuer();
    private final LoginService login = new LoginService(fakes, fakes, fakes, sessions);
    private final RefreshSessionService refresh = new RefreshSessionService(fakes, fakes, fakes, sessions, fakes.clock());
    private final LogoutService logout = new LogoutService(fakes, fakes.clock());

    private User marina;
    private LoginResult loggedIn;

    @BeforeEach
    void login() {
        marina = fakes.addUser("Marina", true);
        fakes.addMembership(marina, "Sakura Sushi");
        loggedIn = login.login(new LoginCommand("marina@sakura.com", "sushi2026bom"));
    }

    @Test
    void rotatesTheRefreshToken() {
        LoginResult renewed = refresh.refresh(loggedIn.refreshToken());

        assertThat(renewed.refreshToken()).isNotEqualTo(loggedIn.refreshToken());
        assertThat(renewed.userId()).isEqualTo(marina.id());
        assertThat(fakes.refreshTokens.values()).extracting(RefreshToken::isRevoked).containsExactly(true, false);
    }

    @Test
    void reusingAnOldTokenEndsTheWholeSession() {
        LoginResult renewed = refresh.refresh(loggedIn.refreshToken());

        // Alguém usa o token antigo (copiado): recusa e derruba a família inteira.
        assertThatThrownBy(() -> refresh.refresh(loggedIn.refreshToken())).isInstanceOf(InvalidSessionException.class);
        assertThatThrownBy(() -> refresh.refresh(renewed.refreshToken())).isInstanceOf(InvalidSessionException.class);
        assertThat(fakes.refreshTokens.values()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void expiredTokenIsRefused() {
        fakes.advance(AccountFakes.REFRESH_TTL);

        assertThatThrownBy(() -> refresh.refresh(loggedIn.refreshToken())).isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void eachRenewalRestartsTheClock() {
        fakes.advance(Duration.ofDays(10));
        LoginResult renewed = refresh.refresh(loggedIn.refreshToken());
        fakes.advance(Duration.ofDays(10)); // 20 dias depois do login, 10 da renovação

        assertThat(refresh.refresh(renewed.refreshToken()).userId()).isEqualTo(marina.id());
    }

    @Test
    void personWhoLostAccessCannotRenew() {
        fakes.memberships.clear(); // desligada do restaurante

        assertThatThrownBy(() -> refresh.refresh(loggedIn.refreshToken())).isInstanceOf(InvalidSessionException.class);
        assertThat(fakes.refreshTokens.values()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void unknownOrMissingTokenIsRefused() {
        assertThatThrownBy(() -> refresh.refresh("inventado")).isInstanceOf(InvalidSessionException.class);
        assertThatThrownBy(() -> refresh.refresh(null)).isInstanceOf(InvalidSessionException.class);
        assertThatThrownBy(() -> refresh.refresh(" ")).isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void logoutEndsTheSession() {
        LoginResult renewed = refresh.refresh(loggedIn.refreshToken());

        logout.logout(renewed.refreshToken());

        assertThatThrownBy(() -> refresh.refresh(renewed.refreshToken())).isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void logoutWithoutTokenIsNotAnError() {
        logout.logout(null);
        logout.logout("inventado");

        assertThat(fakes.refreshTokens.values()).noneMatch(RefreshToken::isRevoked);
    }
}