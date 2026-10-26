package br.com.cardapio.domain.account.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Um refresh token emitido para uma pessoa num estabelecimento.
 *
 * Cada login abre uma família (familyId = id do primeiro token). Cada renovação troca o token
 * por um novo da mesma família e revoga o anterior. Se um token já revogado aparecer de novo,
 * alguém copiou o token: a família inteira é revogada.
 */
public record RefreshToken(
        RefreshTokenId id,
        RefreshTokenId familyId,
        UserId userId,
        EstablishmentId establishmentId,
        RefreshTokenHash hash,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {
    public RefreshToken {
        Objects.requireNonNull(id);
        Objects.requireNonNull(familyId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(hash);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("o refresh token precisa vencer depois de ser criado");
        }
    }

    /** Primeiro token de uma sessão (login). */
    public static RefreshToken issue(
            UserId userId,
            EstablishmentId establishmentId,
            RefreshTokenHash hash,
            Instant now,
            Duration ttl
    ) {
        RefreshTokenId id = RefreshTokenId.newId();
        return new RefreshToken(id, id, userId, establishmentId, hash, now, now.plus(ttl), null);
    }

    /** O próximo token da mesma sessão (renovação). */
    public RefreshToken rotate(RefreshTokenHash newHash, Instant now, Duration ttl) {
        return new RefreshToken(RefreshTokenId.newId(), familyId, userId, establishmentId, newHash,
                now, now.plus(ttl), null);
    }

    public RefreshToken revoke(Instant now) {
        if (isRevoked()) {
            return this;
        }
        return new RefreshToken(id, familyId, userId, establishmentId, hash, createdAt, expiresAt, now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
