package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.UnlinkDeviceUseCase;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.exception.DeviceNotFoundException;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class UnlinkDeviceService implements UnlinkDeviceUseCase {

    private final DevicePort devices;
    private final Clock clock;

    public UnlinkDeviceService(DevicePort devices, Clock clock) {
        this.devices = devices;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void unlink(EstablishmentId establishmentId, DeviceId deviceId) {
        Device device = devices.find(establishmentId, deviceId).orElseThrow(DeviceNotFoundException::new);
        if (device.isActive()) {
            devices.save(device.revoke(clock.instant()));
        }
    }
}
