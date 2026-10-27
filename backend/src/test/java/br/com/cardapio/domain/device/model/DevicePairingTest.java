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
}
