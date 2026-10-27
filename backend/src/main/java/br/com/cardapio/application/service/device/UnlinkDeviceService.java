package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.UnlinkDeviceUseCase;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.device.DeviceSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.exception.DeviceNotFoundException;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class UnlinkDeviceService implements UnlinkDeviceUseCase {

    private final DevicePort devices;
    private final DeviceSessionPort deviceSessions;
    private final Clock clock;

    public UnlinkDeviceService(DevicePort devices, DeviceSessionPort deviceSessions, Clock clock) {
        this.devices = devices;
        this.deviceSessions = deviceSessions;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void unlink(EstablishmentId establishmentId, DeviceId deviceId) {
        Device device = devices.find(establishmentId, deviceId).orElseThrow(DeviceNotFoundException::new);
        if (device.isActive()) {
            Instant now = clock.instant();
            devices.save(device.revoke(now));
            // Quem entrou por PIN neste aparelho sai junto: a renovação da sessão é recusada,
            // e o token de acesso que já está na mão morre em até 15 minutos.
            deviceSessions.revokeAll(device.id(), now);
        }
    }
}
