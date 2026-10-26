package br.com.cardapio.domain.account.model;


import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final Duration TTL = Duration.ofDays(14);

    private final RefreshToken first = RefreshToken.issue(UserId.newId(), EstablishmentId.newId(),
            RefreshTokenHash.of("primeiro"), NOW, TTL);

    @Test
    void loginOpensANewFamily() {
        assertThat(first.familyId()).isEqualTo(first.id());
        assertThat(first.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(first.isRevoked()).isFalse();
    }

    @Test
    void rotationKeepsTheFamilyAndRestartsTheClock() {
        Instant later = NOW.plus(Duration.ofDays(3));
        RefreshToken next = first.rotate(RefreshTokenHash.of("segundo"), later, TTL);

        assertThat(next.id()).isNotEqualTo(first.id());
        assertThat(next.familyId()).isEqualTo(first.familyId());
        assertThat(next.userId()).isEqualTo(first.userId());
        assertThat(next.establishmentId()).isEqualTo(first.establishmentId());
        assertThat(next.expiresAt()).isEqualTo(later.plus(TTL));
    }

    @Test
    void revokeKeepsTheFirstRevocationTime() {
        RefreshToken revoked = first.revoke(NOW);

        assertThat(revoked.isRevoked()).isTrue();
        assertThat(revoked.revoke(NOW.plusSeconds(60)).revokedAt()).isEqualTo(NOW);
    }

    @Test
    void expiresExactlyAtExpiresAt() {
        assertThat(first.isExpired(NOW.plus(TTL).minusSeconds(1))).isFalse();
        assertThat(first.isExpired(NOW.plus(TTL))).isTrue();
    }
}