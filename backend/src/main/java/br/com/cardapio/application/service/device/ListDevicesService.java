package br.com.cardapio.application.service.device;

import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.in.device.ListDevicesUseCase;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListDevicesService implements ListDevicesUseCase {

    private final DevicePort devices;

    public ListDevicesService(DevicePort devices) {
        this.devices = devices;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<DeviceSummary> list(EstablishmentId establishmentId, PageQuery page) {
        return devices.findActive(establishmentId, page).map(DeviceSummary::from);
    }
}
