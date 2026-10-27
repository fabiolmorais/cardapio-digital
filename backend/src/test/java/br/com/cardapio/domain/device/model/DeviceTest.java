package br.com.cardapio.domain.device.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void linkedDeviceStartsWithoutTokenUntilItComesToFetchIt() {
        Device device = Device.link(sakura, "  Celular do salão ", DeviceType.WAITER, NOW);

        assertThat(device.name()).isEqualTo("Celular do salão");
        assertThat(device.tokenHash()).isNull();
        assertThat(device.lastSeenAt()).isNull();
        assertThat(device.isActive()).isTrue();

        Device withToken = device.issueToken(DeviceSecretHash.of("token"), NOW.plusSeconds(5));
        assertThat(withToken.tokenHash()).isNotNull();
        assertThat(withToken.lastSeenAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void revokeKeepsTheRecordAndIsIdempotent() {
        Device device = Device.link(sakura, "TV do salão", DeviceType.TV, NOW);

        Device revoked = device.revoke(NOW.plusSeconds(60));

        assertThat(revoked.isActive()).isFalse();
        assertThat(revoked.revoke(NOW.plusSeconds(120)).revokedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void seenUpdatesTheLastUseAndTvDoesNotTakePins() {
        Device tv = Device.link(sakura, "TV do salão", DeviceType.TV, NOW);

        assertThat(tv.seen(NOW.plusSeconds(30)).lastSeenAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(tv.acceptsPinLogin()).isFalse();
        assertThat(Device.link(sakura, "Tablet", DeviceType.KITCHEN, NOW).acceptsPinLogin()).isTrue();
    }

    @Test
    void needsAName() {
        assertThatThrownBy(() -> Device.link(sakura, " ", DeviceType.KITCHEN, NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Device.link(sakura, "x".repeat(61), DeviceType.KITCHEN, NOW))
                .isInstanceOf(InvalidValueException.class);
    }
}
