package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.in.account.SwitchEstablishmentUseCase;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.RefreshTokenPort;
import br.com.cardapio.domain.account.exception.AccessRevokedException;
import br.com.cardapio.domain.account.exception.UnitNotFoundException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshTokenHash;
import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.account.model.UserId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class SwitchEstablishmentService implements SwitchEstablishmentUseCase {

    private final LoadUserPort users;
    private final LoadMembershipPort memberships;
    private final RefreshTokenPort refreshTokens;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    public SwitchEstablishmentService(LoadUserPort users, LoadMembershipPort memberships,
                                      RefreshTokenPort refreshTokens, SessionIssuer sessionIssuer, Clock clock) {
        this.users = users;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.sessionIssuer = sessionIssuer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public LoginResult switchTo(UserId userId, String rawRefreshToken, EstablishmentId target) {
        User user = users.findById(userId).filter(User::active).orElseThrow(AccessRevokedException::new);
        // Sem vínculo ativo (de outra empresa, desligado ou unidade desativada): 404, sem dizer qual.
        ActiveMembership membership = memberships.findActive(userId, target).orElseThrow(UnitNotFoundException::new);

        // A sessão de agora acaba: o cookie dela não renova mais (e não volta para a unidade antiga).
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokens.findByHash(RefreshTokenHash.of(rawRefreshToken))
                    .filter(token -> token.userId().equals(userId))
                    .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
        }
        return sessionIssuer.start(user, membership);
    }
}
