package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.ClaimResult;
import br.com.cardapio.application.port.in.device.DeviceInvite;
import br.com.cardapio.application.port.in.device.InviteDeviceCommand;
import br.com.cardapio.application.port.out.floor.DisplayContent;
import br.com.cardapio.application.port.out.floor.TableDisplayRenderer;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.exception.PairingNotFoundException;
import br.com.cardapio.domain.device.model.DeviceType;
import br.com.cardapio.domain.device.model.PairingId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O convite por QR Code: o gerente mostra o QR, e o aparelho que escanear fica vinculado. */
class InviteDeviceServiceTest {

    private static final Duration INVITE_TTL = Duration.ofMinutes(5);

    /** O "QR" é o próprio endereço: assim o teste confere o que vai dentro dele. */
    private static final TableDisplayRenderer QR = new TableDisplayRenderer() {
        @Override public byte[] qrPng(String url) { throw new UnsupportedOperationException(); }
        @Override public byte[] qrSvg(String url) { return ("<svg>" + url + "</svg>").getBytes(StandardCharsets.UTF_8); }
        @Override public byte[] displaysPdf(List<DisplayContent> displays) { throw new UnsupportedOperationException(); }
    };

    private final DeviceFakes fakes = new DeviceFakes();
    private final InviteDeviceService invite = new InviteDeviceService(fakes, fakes, fakes, fakes, QR, fakes.clock(),
            INVITE_TTL, "https://ohchefe.app.br/");
    private final ClaimPairingService claim = new ClaimPairingService(fakes, fakes, fakes, fakes.clock());
    private final EstablishmentId sakura = EstablishmentId.newId();

    private DeviceInvite inviteKitchenTablet() {
        return invite.invite(new InviteDeviceCommand(sakura, " Tablet da cozinha ", DeviceType.KITCHEN));
    }

    private ClaimResult scan(DeviceInvite invite) {
        String[] parts = invite.url().substring(invite.url().indexOf("#convite=") + 9).split("\\.");
        return claim.claim(new PairingId(UUID.fromString(parts[0])), parts[1]);
    }

    @Test
    void theQrCodeLinksTheDeviceOnTheFirstScan() {
        DeviceInvite invite = inviteKitchenTablet();

        assertThat(invite.url()).startsWith("https://ohchefe.app.br/aparelho#convite=");
        assertThat(invite.qrSvg()).isEqualTo("<svg>" + invite.url() + "</svg>");
        assertThat(invite.expiresAt()).isEqualTo(fakes.clock().instant().plus(INVITE_TTL));
        assertThat(invite.device().name()).isEqualTo("Tablet da cozinha");
        assertThat(invite.device().lastSeenAt()).isNull(); // "Aguardando o aparelho"

        ClaimResult result = scan(invite);
        assertThat(result).isInstanceOfSatisfying(ClaimResult.Linked.class, linked -> {
            assertThat(linked.device().id()).isEqualTo(invite.device().id());
            assertThat(linked.device().type()).isEqualTo(DeviceType.KITCHEN);
        });
        assertThat(fakes.devices.get(invite.device().id()).establishmentId()).isEqualTo(sakura);
        assertThatThrownBy(() -> scan(invite)).isInstanceOf(PairingNotFoundException.class); // uma vez só
    }

    @Test
    void theQrCodeExpiresEvenThoughItIsAlreadyLinked() {
        DeviceInvite invite = inviteKitchenTablet();
        fakes.advance(INVITE_TTL);

        assertThatThrownBy(() -> scan(invite)).isInstanceOf(PairingNotFoundException.class);
    }

    @Test
    void theSecretNeverShowsInTheLogs() {
        DeviceInvite invite = inviteKitchenTablet();

        assertThat(invite.toString()).doesNotContain("#convite=").doesNotContain("svg");
    }

    @Test
    void theQrCodeCodeIsNotOneTheManagerCanType() {
        DeviceInvite invite = inviteKitchenTablet();

        // O pedido já nasce vinculado: o código dele não aparece como aberto para ninguém digitar.
        assertThat(fakes.pairings.values()).singleElement().satisfies(p -> {
            assertThat(p.invite()).isTrue();
            assertThat(p.isOpen(fakes.clock().instant())).isFalse();
            assertThat(p.deviceId()).isEqualTo(invite.device().id());
        });
    }
}
