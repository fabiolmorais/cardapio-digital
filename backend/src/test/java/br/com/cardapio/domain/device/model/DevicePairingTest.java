package br.com.cardapio.domain.device.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DevicePairingTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final DevicePairing pairing = DevicePairing.start(new PairingCode("482719"),
            DeviceSecretHash.of("segredo-do-aparelho"), NOW, TTL);

    @Test
    void startsOpenAndExpiresAfterTheTtl() {
        assertThat(pairing.isOpen(NOW)).isTrue();
        assertThat(pairing.isOpen(NOW.plus(TTL).minusSeconds(1))).isTrue();
        assertThat(pairing.isExpired(NOW.plus(TTL))).isTrue();
        assertThat(pairing.isOpen(NOW.plus(TTL))).isFalse();
    }

    @Test
    void onlyTheRightSecretBelongsToIt() {
        assertThat(pairing.belongsTo("segredo-do-aparelho")).isTrue();
        assertThat(pairing.belongsTo("outro")).isFalse();
        assertThat(pairing.belongsTo(null)).isFalse();
        assertThat(pairing.belongsTo(" ")).isFalse();
    }

    @Test
    void theDatabaseKeepsOnlyTheHashOfTheSecret() {
        assertThat(pairing.secretHash().value()).hasSize(64).doesNotContain("segredo");
    }

    @Test
    void aLinkedCodeStillGetsItsTokenAfterTheTtlButAnInviteDoesNot() {
        Instant late = NOW.plus(TTL);
        DevicePairing linkedByCode = new DevicePairing(pairing.id(), pairing.code(), pairing.secretHash(),
                pairing.createdAt(), pairing.expiresAt(), DeviceId.newId(), null);
        DevicePairing invite = DevicePairing.invite(new PairingCode("130557"), DeviceSecretHash.of("do-qr"),
                DeviceId.newId(), NOW, TTL);

        assertThat(pairing.expiredForClaim(late)).isTrue();
        assertThat(linkedByCode.expiredForClaim(late)).isFalse();
        assertThat(invite.isLinked()).isTrue();
        assertThat(invite.expiredForClaim(late.minusSeconds(1))).isFalse();
        assertThat(invite.expiredForClaim(late)).isTrue();
    }
}
