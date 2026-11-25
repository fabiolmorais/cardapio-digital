package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginCommand;
import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.domain.account.exception.AccessRevokedException;
import br.com.cardapio.domain.account.exception.UnitNotFoundException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshToken;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwitchEstablishmentServiceTest {

    private final AccountFakes fakes = new AccountFakes();
    private final SessionIssuer sessions = fakes.sessionIssuer();
    private final LoginService login = new LoginService(fakes, fakes, fakes, sessions);
    private final SwitchEstablishmentService switcher =
            new SwitchEstablishmentService(fakes, fakes, fakes, sessions, fakes.clock());

    private User marina;
    private ActiveMembership moema;
    private LoginResult loggedIn;

    @BeforeEach
    void login() {
        marina = fakes.addUser("Marina", true);
        fakes.addMembership(marina, "Sakura Pinheiros");
        moema = fakes.addMembership(marina, "Sakura Moema");
        loggedIn = login.login(new LoginCommand("marina@sakura.com", "sushi2026bom"));
    }

    @Test
    void opensASessionInTheChosenUnitAndEndsTheCurrentOne() {
        LoginResult switched = switcher.switchTo(marina.id(), loggedIn.refreshToken(), moema.establishmentId());

        assertThat(switched.establishment().establishmentId()).isEqualTo(moema.establishmentId());
        assertThat(switched.establishment().name()).isEqualTo("Sakura Moema");
        assertThat(fakes.lastSubject.establishmentId()).isEqualTo(moema.establishmentId());
        // A família antiga foi encerrada; a nova está de pé.
        assertThat(fakes.refreshTokens.values()).extracting(RefreshToken::isRevoked).containsExactly(true, false);
    }

    @Test
    void withoutAMembershipItIs404AndTheSessionStays() {
        assertThatThrownBy(() -> switcher.switchTo(marina.id(), loggedIn.refreshToken(), EstablishmentId.newId()))
                .isInstanceOf(UnitNotFoundException.class);
        assertThat(fakes.refreshTokens.values()).extracting(RefreshToken::isRevoked).containsExactly(false);
    }

    @Test
    void someoneElsesCookieIsNotTouched() {
        User carlos = fakes.addUser("Carlos", true);
        fakes.addMembership(carlos, moema.establishmentId(), Role.OWNER);

        switcher.switchTo(carlos.id(), loggedIn.refreshToken(), moema.establishmentId());

        assertThat(fakes.refreshTokens.values()).extracting(RefreshToken::isRevoked).containsExactly(false, false);
    }

    @Test
    void aDeactivatedPersonCannotSwitch() {
        User off = fakes.addUser("Rodrigo", false);
        fakes.addMembership(off, moema.establishmentId(), Role.OWNER);

        assertThatThrownBy(() -> switcher.switchTo(off.id(), null, moema.establishmentId()))
                .isInstanceOf(AccessRevokedException.class);
    }
}
