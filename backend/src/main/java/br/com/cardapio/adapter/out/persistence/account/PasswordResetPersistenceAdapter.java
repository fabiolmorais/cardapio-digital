package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.account.PasswordResetPort;
import br.com.cardapio.domain.account.model.PasswordHash;
import br.com.cardapio.domain.account.model.PasswordResetToken;
import br.com.cardapio.domain.account.model.PasswordResetTokenId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.model.TokenHash;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class PasswordResetPersistenceAdapter implements PasswordResetPort {

    private final PasswordResetTokenJpaRepository resets;
    private final UserJpaRepository users;
    private final RefreshTokenJpaRepository refreshTokens;

    PasswordResetPersistenceAdapter(PasswordResetTokenJpaRepository resets, UserJpaRepository users,
                                    RefreshTokenJpaRepository refreshTokens) {
        this.resets = resets;
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @Override
    public void save(PasswordResetToken t) {
        resets.save(new PasswordResetTokenJpaEntity(t.id().value(), t.userId().value(), t.hash().value(),
                t.createdAt(), t.expiresAt(), t.usedAt()));
    }

    @Override
    public Optional<PasswordResetToken> findByHash(TokenHash hash) {
        return resets.findByTokenHash(hash.value()).map(e -> new PasswordResetToken(new PasswordResetTokenId(e.getId()),
                new UserId(e.getUserId()), new TokenHash(e.getTokenHash()), e.getCreatedAt(), e.getExpiresAt(),
                e.getUsedAt()));
    }

    @Override
    public long countSince(UserId userId, Instant since) {
        return resets.countByUserIdAndCreatedAtGreaterThanEqual(userId.value(), since);
    }

    @Override
    public void changePassword(UserId userId, PasswordHash newHash, Instant now) {
        users.changePassword(userId.value(), newHash.value());
        resets.useAllOf(userId.value(), now);
        refreshTokens.revokeAllOf(userId.value(), now);
    }
}
