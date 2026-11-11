package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.DeviceInvite;
import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.in.device.InviteDeviceCommand;
import br.com.cardapio.application.port.in.device.InviteDeviceUseCase;
import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.floor.TableDisplayRenderer;
import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DevicePairing;
import br.com.cardapio.domain.device.model.DeviceSecretHash;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * O convite por QR Code. O aparelho nasce como no vínculo por código ("Aguardando o aparelho"),
 * e o pedido de vínculo já aponta para ele. O QR leva o endereço da tela do aparelho com o id e o
 * segredo do pedido; ao abrir, o aparelho busca o token como no fluxo do código (uma vez só).
 *
 * O QR é desenhado pelo mesmo gerador do QR das mesas.
 */
public class InviteDeviceService implements InviteDeviceUseCase {

    /** A tela do aparelho no front; o convite vai depois do #, que não sai do navegador. */
    static final String PATH = "/aparelho#convite=";

    private final DevicePairingPort pairings;
    private final DevicePort devices;
    private final RandomCodeGenerator codes;
    private final RandomTokenGenerator secrets;
    private final TableDisplayRenderer qrCodes;
    private final Clock clock;
    private final Duration ttl;
    private final String publicUrl;

    public InviteDeviceService(DevicePairingPort pairings, DevicePort devices, RandomCodeGenerator codes,
                               RandomTokenGenerator secrets, TableDisplayRenderer qrCodes, Clock clock, Duration ttl,
                               String publicUrl) {
        this.pairings = pairings;
        this.devices = devices;
        this.codes = codes;
        this.secrets = secrets;
        this.qrCodes = qrCodes;
        this.clock = clock;
        this.ttl = Objects.requireNonNull(ttl);
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    @Override
    @Transactional
    public DeviceInvite invite(InviteDeviceCommand command) {
        Instant now = clock.instant();
        Device device = Device.link(command.establishmentId(), command.name(), command.type(), now);
        devices.save(device);

        String secret = secrets.generate();
        DevicePairing pairing = DevicePairing.invite(PairingCodes.free(pairings, codes, now),
                DeviceSecretHash.of(secret), device.id(), now, ttl);
        pairings.save(pairing);

        String url = publicUrl + PATH + pairing.id().value() + "." + secret;
        String svg = new String(qrCodes.qrSvg(url), StandardCharsets.UTF_8);
        return new DeviceInvite(DeviceSummary.from(device), url, svg, pairing.expiresAt());
    }
}
