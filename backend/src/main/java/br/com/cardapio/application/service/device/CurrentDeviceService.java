package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.CurrentDevice;
import br.com.cardapio.application.port.in.device.CurrentDeviceUseCase;
import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import org.springframework.transaction.annotation.Transactional;

public class CurrentDeviceService implements CurrentDeviceUseCase {

    private final DeviceAuthenticator authenticator;

    public CurrentDeviceService(DeviceAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    /** Não é readOnly: grava o "visto agora" do aparelho. */
    @Override
    @Transactional
    public CurrentDevice current(String deviceToken) {
        LinkedDevice linked = authenticator.authenticate(deviceToken);
        return new CurrentDevice(DeviceSummary.from(linked.device()), linked.establishmentName());
    }
}
