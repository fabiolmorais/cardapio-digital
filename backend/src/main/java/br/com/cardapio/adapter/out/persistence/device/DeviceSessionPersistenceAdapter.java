package br.com.cardapio.adapter.out.persistence.device;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.device.DeviceSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import br.com.cardapio.domain.device.model.DeviceId;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class DeviceSessionPersistenceAdapter implements DeviceSessionPort {

    private final DeviceSessionJpaRepository sessions;
    private final EstablishmentFilter establishmentFilter;

    DeviceSessionPersistenceAdapter(DeviceSessionJpaRepository sessions, EstablishmentFilter establishmentFilter) {
        this.sessions = sessions;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void register(RefreshTokenId familyId, DeviceId deviceId, EstablishmentId establishmentId, Instant now) {
        establishmentFilter.enable(establishmentId);
        sessions.save(new DeviceSessionJpaEntity(familyId.value(), deviceId.value(), establishmentId.value(), now));
    }

    @Override
    public void revokeAll(DeviceId deviceId, Instant now) {
        sessions.revokeAll(deviceId.value(), now);
    }
}
