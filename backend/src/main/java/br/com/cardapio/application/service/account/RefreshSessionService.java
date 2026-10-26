package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.in.account.RefreshSessionUseCase;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.RefreshTokenPort;
import br.com.cardapio.domain.account.exception.InvalidSessionException;
import br.com.cardapio.domain.account.model.RefreshToken;
import br.com.cardapio.domain.account.model.RefreshTokenHash;
import br.com.cardapio.domain.account.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class RefreshSessionService implements RefreshSessionUseCase {

    private final RefreshTokenPort refreshTokens;
    private final LoadUserPort loadUserPort;
    private final LoadMembershipPort loadMembershipPort;
    private final SessionIssuer sessionIssuer;
    private final Clock clock;

    public RefreshSessionService(RefreshTokenPort refreshTokens, LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort, SessionIssuer sessionIssuer, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.loadUserPort = loadUserPort;
        this.loadMembershipPort = loadMembershipPort;
        this.sessionIssuer = sessionIssuer;
        this.clock = clock;
    }

    /**
     * noRollbackFor: quando a renovação é recusada depois de revogar a família, a revogação
     * precisa ficar gravada. Sem isso, a exceção desfaria o UPDATE.
     */
    @Override
    @Transactional(noRollbackFor = InvalidSessionException.class)
    public LoginResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidSessionException();
        }
        RefreshToken current = refreshTokens.findByHash(RefreshTokenHash.of(rawToken))
                .orElseThrow(InvalidSessionException::new);
        Instant now = clock.instant();

        if (current.isRevoked()) {
            // Token já trocado aparecendo de novo: alguém tem uma cópia. Encerra a sessão inteira.
            refreshTokens.revokeFamily(current.familyId(), now);
            throw new InvalidSessionException();
        }
        if (current.isExpired(now)) {
            throw new InvalidSessionException();
        }

        Optional<User> user = loadUserPort.findById(current.userId()).filter(User::active);
        Optional<ActiveMembership> membership = loadMembershipPort.findActive(current.userId(), current.establishmentId());
        if (user.isEmpty() || membership.isEmpty()) {
            // Pessoa desligada ou restaurante desativado: a sessão acaba aqui.
            refreshTokens.revokeFamily(current.familyId(), now);
            throw new InvalidSessionException();
        }

        return sessionIssuer.rotate(current, user.get(), membership.get());
    }
}
