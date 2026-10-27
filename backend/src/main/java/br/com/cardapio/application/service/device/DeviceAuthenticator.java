package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import br.com.cardapio.domain.device.exception.InvalidDeviceException;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceSecretHash;

import java.time.Clock;

/**
 * Descobre que aparelho está chamando pelo token do cookie e marca que ele foi visto.
 * Não é caso de uso: é a peça que a tela do PIN e o login por PIN usam, dentro da transação
 * de quem chama (como o SessionIssuer do Guia 3).
 */
public class DeviceAuthenticator {

    private final DevicePort devices;
    private final Clock clock;

    public DeviceAuthenticator(DevicePort devices, Clock clock) {
        this.devices = devices;
        this.clock = clock;
    }

    public LinkedDevice authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidDeviceException();
        }
        LinkedDevice linked = devices.findActiveByTokenHash(DeviceSecretHash.of(rawToken))
                .orElseThrow(InvalidDeviceException::new);
        Device seen = linked.device().seen(clock.instant());
        devices.save(seen);
        return new LinkedDevice(seen, linked.establishmentName());
    }
}
