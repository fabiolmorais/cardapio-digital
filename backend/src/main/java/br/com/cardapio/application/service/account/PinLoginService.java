package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.in.account.PinLoginCommand;
import br.com.cardapio.application.port.in.account.PinLoginUseCase;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.PinLockoutPort;
import br.com.cardapio.application.port.out.device.DeviceSessionPort;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.service.device.DeviceAuthenticator;
import br.com.cardapio.domain.account.exception.AccessRevokedException;
import br.com.cardapio.domain.account.exception.InvalidPinException;
import br.com.cardapio.domain.account.exception.PinLockedException;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.PinLockout;
import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.device.exception.PinNotAllowedOnDeviceException;
import br.com.cardapio.domain.device.model.Device;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class PinLoginService implements PinLoginUseCase {

    private final DeviceAuthenticator authenticator;
    private final TeamMemberPort team;
    private final PinLockoutPort pinLockouts;
    private final PasswordHasher hasher;
    private final LoadUserPort users;
    private final LoadMembershipPort memberships;
    private final SessionIssuer sessionIssuer;
    private final DeviceSessionPort deviceSessions;
    private final Clock clock;

    public PinLoginService(DeviceAuthenticator authenticator, TeamMemberPort team, PinLockoutPort pinLockouts,
                           PasswordHasher hasher, LoadUserPort users, LoadMembershipPort memberships,
                           SessionIssuer sessionIssuer, DeviceSessionPort deviceSessions, Clock clock) {
        this.authenticator = authenticator;
        this.team = team;
        this.pinLockouts = pinLockouts;
        this.hasher = hasher;
        this.users = users;
        this.memberships = memberships;
        this.sessionIssuer = sessionIssuer;
        this.deviceSessions = deviceSessions;
        this.clock = clock;
    }

    /**
     * noRollbackFor: o erro de PIN precisa ficar gravado (é ele que conta para o bloqueio),
     * mesmo com a exceção. É o mesmo cuidado do refresh token no Guia 3.
     */
    @Override
    @Transactional(noRollbackFor = {InvalidPinException.class, PinLockedException.class})
    public LoginResult login(PinLoginCommand command) {
        Device device = authenticator.authenticate(command.deviceToken()).device();
        if (!device.acceptsPinLogin()) {
            throw new PinNotAllowedOnDeviceException();
        }
        // O restaurante sai do aparelho, nunca do pedido.
        EstablishmentId establishmentId = device.establishmentId();
        Membership membership = team.findMember(establishmentId, command.memberId())
                .map(TeamMember::membership)
                .filter(Membership::canUsePin)
                .orElseThrow(TeamMemberNotFoundException::new);

        Instant now = clock.instant();
        PinLockout lockout = pinLockouts.lockAndFind(establishmentId, membership.id())
                .orElseGet(() -> PinLockout.clean(membership.id(), establishmentId));
        if (lockout.isLocked(now)) {
            throw new PinLockedException(lockout.minutesLeft(now));
        }

        if (command.pin() == null || !hasher.matches(command.pin(), membership.pinHash())) {
            PinLockout failed = lockout.registerFailure(now);
            pinLockouts.save(failed);
            if (failed.isLocked(now)) {
                throw new PinLockedException(failed.minutesLeft(now));
            }
            throw new InvalidPinException(failed.remainingAttempts());
        }
        if (lockout.hasFailures()) {
            pinLockouts.save(lockout.reset());
        }

        User user = users.findById(membership.userId()).filter(User::active).orElseThrow(AccessRevokedException::new);
        ActiveMembership active = memberships.findActive(user.id(), establishmentId)
                .orElseThrow(AccessRevokedException::new);

        SessionIssuer.IssuedSession session = sessionIssuer.startFamily(user, active);
        deviceSessions.register(session.familyId(), device.id(), establishmentId, now);
        return session.result();
    }
}
