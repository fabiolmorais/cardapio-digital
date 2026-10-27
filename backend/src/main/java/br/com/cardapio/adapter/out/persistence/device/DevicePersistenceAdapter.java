package br.com.cardapio.adapter.out.persistence.device;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.device.DevicePort;
import br.com.cardapio.application.port.out.device.LinkedDevice;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Aparelhos de um restaurante, com a defesa em dobro do Guia 4 (WHERE + filtro). A única
 * exceção é findForClaim, que roda antes de o aparelho saber de que restaurante é.
 */
@Component
class DevicePersistenceAdapter implements DevicePort {

    private final DeviceJpaRepository devices;
    private final EstablishmentFilter establishmentFilter;

    DevicePersistenceAdapter(DeviceJpaRepository devices, EstablishmentFilter establishmentFilter) {
        this.devices = devices;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void save(Device device) {
        establishmentFilter.enable(device.establishmentId());
        devices.save(DevicePersistenceMapper.toEntity(device));
    }

    @Override
    public ResultPage<Device> findActive(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<DeviceJpaEntity> rows = devices.findActive(establishmentId.value(),
                PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(DevicePersistenceMapper::toDomain).getContent(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public Optional<Device> find(EstablishmentId establishmentId, DeviceId id) {
        establishmentFilter.enable(establishmentId);
        return devices.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(DevicePersistenceMapper::toDomain);
    }

    @Override
    public Optional<LinkedDevice> findForClaim(DeviceId id) {
        return devices.findById(id.value())
                .map(DevicePersistenceMapper::toDomain)
                .flatMap(device -> devices.findEstablishmentName(device.establishmentId().value())
                        .map(name -> new LinkedDevice(device, name)));
    }
}
