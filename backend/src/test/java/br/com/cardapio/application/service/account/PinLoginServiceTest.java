package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.in.account.PinLoginCommand;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.service.device.DeviceAuthenticator;
import br.com.cardapio.domain.account.exception.InvalidPinException;
import br.com.cardapio.domain.account.exception.PinLockedException;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.device.exception.InvalidDeviceException;
import br.com.cardapio.domain.device.exception.PinNotAllowedOnDeviceException;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceSecretHash;
import br.com.cardapio.domain.device.model.DeviceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinLoginServiceTest {

    private static final String DEVICE_TOKEN = "token-do-celular";
    private static final String PIN = "2580";

    private final AccountFakes accounts = new AccountFakes();
    private final PinLoginFakes pins = new PinLoginFakes();
    private final PinLoginService service = new PinLoginService(new DeviceAuthenticator(pins, accounts.clock()),
            pins, pins, accounts, accounts, accounts, accounts.sessionIssuer(), pins, accounts.clock());

    private final EstablishmentId sakura = EstablishmentId.newId();
    private Device phone;
    private Membership carlos;

    @BeforeEach
    void waiterAndPhone() {
        phone = givenDevice(DeviceType.WAITER, DEVICE_TOKEN);
        carlos = givenMember("Carlos", Role.STAFF, sakura);
    }

    private Device givenDevice(DeviceType type, String token) {
        Device device = Device.link(sakura, "Celular do salão", type, Instant.parse("2026-10-27T10:00:00Z"))
                .issueToken(DeviceSecretHash.of(token), Instant.parse("2026-10-27T10:00:05Z"));
        pins.save(device);
        return device;
    }

    /** A pessoa nos dois lados: na equipe (PIN) e nas contas (usuário + vínculo ativo). */
    private Membership givenMember(String name, Role role, EstablishmentId establishmentId) {
        User user = User.teamMember(name);
        accounts.users.add(user);
        ActiveMembership active = accounts.addMembership(user, establishmentId, role);
        Membership membership = new Membership(active.membershipId(), user.id(), establishmentId, role,
                accounts.hash(PIN), true);
        pins.members.put(membership.id(), new TeamMember(membership, name));
        return membership;
    }

    private LoginResult login(Membership who, String pin) {
        return service.login(new PinLoginCommand(DEVICE_TOKEN, who.id(), pin));
    }

    @Test
    void rightPinOpensASessionTiedToTheDevice() {
        LoginResult result = login(carlos, PIN);

        assertThat(result.userName()).isEqualTo("Carlos");
        assertThat(result.establishment().role()).isEqualTo(Role.STAFF);
        assertThat(accounts.lastSubject.establishmentId()).isEqualTo(sakura); // o restaurante veio do aparelho
        assertThat(pins.sessions).containsValue(phone.id());
        assertThat(pins.devices.get(phone.id()).lastSeenAt()).isEqualTo(accounts.clock().instant());
    }

    @Test
    void wrongPinsCountDownAndTheFifthLocks() {
        for (int remaining = 4; remaining >= 1; remaining--) {
            int expected = remaining;
            assertThatThrownBy(() -> login(carlos, "9999"))
                    .isInstanceOfSatisfying(InvalidPinException.class,
                            e -> assertThat(e.remainingAttempts()).isEqualTo(expected));
        }
        assertThatThrownBy(() -> login(carlos, "9999")).isInstanceOf(PinLockedException.class);
        // Bloqueado, nem o PIN certo entra.
        assertThatThrownBy(() -> login(carlos, PIN))
                .isInstanceOfSatisfying(PinLockedException.class, e -> assertThat(e.minutesLeft()).isEqualTo(15));
    }

    @Test
    void afterTheLockTheRightPinWorksAndClearsTheCount() {
        for (int i = 0; i < PinLockout.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> login(carlos, "9999")).isInstanceOf(RuntimeException.class);
        }
        accounts.advance(PinLockout.LOCK_DURATION);

        assertThat(login(carlos, PIN).userName()).isEqualTo("Carlos");
        assertThat(pins.lockouts.get(carlos.id()).hasFailures()).isFalse();
    }

    @Test
    void everyAttemptTakesTheLock() {
        login(carlos, PIN);
        assertThatThrownBy(() -> login(carlos, "9999")).isInstanceOf(InvalidPinException.class);

        assertThat(pins.locks).isEqualTo(2);
    }

    @Test
    void unknownMissingOrUnlinkedDeviceIsRefused() {
        assertThatThrownBy(() -> service.login(new PinLoginCommand("inventado", carlos.id(), PIN)))
                .isInstanceOf(InvalidDeviceException.class);
        assertThatThrownBy(() -> service.login(new PinLoginCommand(null, carlos.id(), PIN)))
                .isInstanceOf(InvalidDeviceException.class);

        pins.save(phone.revoke(Instant.parse("2026-10-27T11:00:00Z")));
        assertThatThrownBy(() -> login(carlos, PIN)).isInstanceOf(InvalidDeviceException.class);
    }

    @Test
    void nobodyEntersWithPinOnTheTv() {
        givenDevice(DeviceType.TV, "token-da-tv");

        assertThatThrownBy(() -> service.login(new PinLoginCommand("token-da-tv", carlos.id(), PIN)))
                .isInstanceOf(PinNotAllowedOnDeviceException.class);
    }

    @Test
    void ownerDeactivatedOrOtherRestaurantMembersCannotUseThisDevice() {
        Membership owner = givenMember("Marina", Role.OWNER, sakura);
        Membership elsewhere = givenMember("Bruno", Role.STAFF, EstablishmentId.newId());
        pins.members.put(carlos.id(), new TeamMember(carlos.deactivate(), "Carlos"));

        assertThatThrownBy(() -> login(owner, PIN)).isInstanceOf(TeamMemberNotFoundException.class);
        assertThatThrownBy(() -> login(elsewhere, PIN)).isInstanceOf(TeamMemberNotFoundException.class);
        assertThatThrownBy(() -> login(carlos, PIN)).isInstanceOf(TeamMemberNotFoundException.class);
    }
}
