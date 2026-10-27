package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.ClaimResult;
import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.in.device.LinkDeviceCommand;
import br.com.cardapio.application.port.in.device.PairingStarted;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.exception.PairingNotFoundException;
import br.com.cardapio.domain.device.model.DeviceSecretHash;
import br.com.cardapio.domain.device.model.DeviceType;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O vínculo inteiro, do jeito que acontece no restaurante: abrir, vincular e buscar o token. */
class DevicePairingFlowTest {

    private final DeviceFakes fakes = new DeviceFakes();
    private final StartPairingService start = new StartPairingService(fakes, fakes, fakes, fakes.clock(), DeviceFakes.TTL);
    private final LinkDeviceService link = new LinkDeviceService(fakes, fakes, fakes.clock());
    private final ClaimPairingService claim = new ClaimPairingService(fakes, fakes, fakes, fakes.clock());
    private final EstablishmentId sakura = EstablishmentId.newId();

    private DeviceSummary linkWithCode(String code) {
        return link.link(new LinkDeviceCommand(sakura, code, "Celular do salão", DeviceType.WAITER));
    }

    @Test
    void openLinkAndClaim() {
        PairingStarted started = start.start();
        assertThat(started.code().value()).isEqualTo("000001");
        assertThat(claim.claim(started.id(), started.secret())).isInstanceOf(ClaimResult.Pending.class);

        DeviceSummary linked = linkWithCode("000001");
        assertThat(linked.lastSeenAt()).isNull(); // ainda não buscou o token

        ClaimResult result = claim.claim(started.id(), started.secret());
        ClaimResult.Linked delivered = (ClaimResult.Linked) result;
        assertThat(delivered.deviceToken()).isEqualTo("token-2"); // token-1 foi o segredo do pedido
        assertThat(delivered.establishmentName()).isEqualTo(DeviceFakes.ESTABLISHMENT_NAME);
        assertThat(delivered.device().id()).isEqualTo(linked.id());
        // O banco guarda só o hash do token, e o aparelho passa a constar como visto.
        var stored = fakes.devices.get(linked.id());
        assertThat(stored.tokenHash()).isEqualTo(DeviceSecretHash.of("token-2"));
        assertThat(stored.lastSeenAt()).isNotNull();
        assertThat(stored.establishmentId()).isEqualTo(sakura);
    }

    @Test
    void theTokenIsDeliveredOnlyOnce() {
        PairingStarted started = start.start();
        linkWithCode(started.code().value());
        claim.claim(started.id(), started.secret());

        assertThatThrownBy(() -> claim.claim(started.id(), started.secret()))
                .isInstanceOf(PairingNotFoundException.class);
    }

    @Test
    void wrongSecretLooksLikeAnUnknownPairing() {
        PairingStarted started = start.start();

        assertThatThrownBy(() -> claim.claim(started.id(), "chutado"))
                .isInstanceOf(PairingNotFoundException.class);
    }

    @Test
    void expiredCodeCannotBeLinkedNorClaimed() {
        PairingStarted started = start.start();
        fakes.advance(DeviceFakes.TTL);

        assertThatThrownBy(() -> linkWithCode(started.code().value())).isInstanceOf(PairingNotFoundException.class);
        assertThatThrownBy(() -> claim.claim(started.id(), started.secret())).isInstanceOf(PairingNotFoundException.class);
        assertThat(fakes.devices).isEmpty();
    }

    @Test
    void aCodeIsUsedOnlyOnce() {
        PairingStarted started = start.start();
        linkWithCode(started.code().value());

        assertThatThrownBy(() -> linkWithCode(started.code().value())).isInstanceOf(PairingNotFoundException.class);
    }

    @Test
    void linkedButNotYetClaimedStillWorksAfterTheCodeExpires() {
        PairingStarted started = start.start();
        linkWithCode(started.code().value());
        fakes.advance(DeviceFakes.TTL.plus(Duration.ofMinutes(1)));

        assertThat(claim.claim(started.id(), started.secret())).isInstanceOf(ClaimResult.Linked.class);
    }

    @Test
    void anOpenCodeIsNeverHandedOutTwice() {
        fakes.nextCodes.push("482719");
        PairingStarted first = start.start();
        fakes.nextCodes.push("482719"); // sorteou o mesmo de novo…
        fakes.nextCodes.addLast("130557"); // …então tenta outro

        PairingStarted second = start.start();

        assertThat(first.code().value()).isEqualTo("482719");
        assertThat(second.code().value()).isEqualTo("130557");
    }

    @Test
    void codeOutOfFormatIsInvalid() {
        assertThatThrownBy(() -> linkWithCode("12ab56")).isInstanceOf(InvalidValueException.class);
    }
}
