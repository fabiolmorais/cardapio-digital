package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.RefreshTokenPort;
import br.com.cardapio.application.port.out.security.AccessToken;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.RefreshToken;
import br.com.cardapio.domain.account.model.RefreshTokenHash;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import br.com.cardapio.domain.account.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Monta a sessão: token de acesso (curto) + refresh token (longo).
 * Usado pelo login (sessão nova) e pela renovação (próximo token da mesma sessão).
 * Não é caso de uso: não tem porta de entrada nem @Transactional; roda dentro da transação de quem chama.
 */
public class SessionIssuer {

    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenPort refreshTokens;
    private final RandomTokenGenerator randomTokens;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public SessionIssuer(AccessTokenIssuer accessTokenIssuer, RefreshTokenPort refreshTokens, RandomTokenGenerator randomTokens, Clock clock, Duration refreshTokenTtl) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokens = refreshTokens;
        this.randomTokens = randomTokens;
        this.clock = clock;
        this.refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl);
    }

    /** Sessão nova e a família de refresh tokens que ela abriu. */
    record IssuedSession(LoginResult result, RefreshTokenId familyId) {
    }

    /** Login: abre uma família nova de refresh tokens. */
    LoginResult start(User user, ActiveMembership membership) {
        return startFamily(user, membership).result();
    }

    /** Como o start, devolvendo também a família (o login por PIN liga a família ao aparelho). */
    IssuedSession startFamily(User user, ActiveMembership membership) {
        Instant now = clock.instant();
        String raw = randomTokens.generate();
        RefreshToken token = RefreshToken.issue(user.id(), membership.establishmentId(),
                RefreshTokenHash.of(raw), now, refreshTokenTtl);
        refreshTokens.save(token);
        return new IssuedSession(result(user, membership, raw, token), token.familyId());
    }

    /** Renovação: emite o próximo token da mesma família. Quem chama já revogou o token usado. */
    LoginResult rotate(RefreshToken current, User user, ActiveMembership membership) {
        Instant now = clock.instant();
        String raw = randomTokens.generate();
        RefreshToken next = current.rotate(RefreshTokenHash.of(raw), now, refreshTokenTtl);
        refreshTokens.save(next);
        return result(user, membership, raw, next);
    }

    private LoginResult result(User user, ActiveMembership membership, String rawRefreshToken, RefreshToken refresh) {
        AccessToken access = accessTokenIssuer.issue(
                new TokenSubject(user.id(), membership.establishmentId(), membership.role()));
        return new LoginResult(access.value(), access.expiresAt(), rawRefreshToken, refresh.expiresAt(),
                user.id(), user.name(), Accesses.from(membership));
    }
}
