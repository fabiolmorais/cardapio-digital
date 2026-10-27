package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.in.device.LinkDeviceCommand;
import br.com.cardapio.application.port.in.device.LinkDeviceUseCase;
import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.domain.device.exception.PairingNotFoundException;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DevicePairing;
import br.com.cardapio.domain.device.model.PairingCode;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class LinkDeviceService implements LinkDeviceUseCase {

    private final DevicePairingPort pairings;
    private final DevicePort devices;
    private final Clock clock;

    public LinkDeviceService(DevicePairingPort pairings, DevicePort devices, Clock clock) {
        this.pairings = pairings;
        this.devices = devices;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DeviceSummary link(LinkDeviceCommand command) {
        Instant now = clock.instant();
        PairingCode code = new PairingCode(command.code());
        DevicePairing pairing = pairings.findOpenByCode(code, now).orElseThrow(PairingNotFoundException::new);

        Device device = Device.link(command.establishmentId(), command.name(), command.type(), now);
        devices.save(device);
        if (!pairings.linkIfOpen(pairing.id(), device.id(), now)) {
            // Outro gerente usou o código entre a busca e agora (ou ele venceu). A exceção
            // desfaz a transação inteira, inclusive o aparelho gravado acima.
            throw new PairingNotFoundException();
        }
        return DeviceSummary.from(device);
    }
}
