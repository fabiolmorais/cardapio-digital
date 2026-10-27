package br.com.cardapio.adapter.out.persistence.device;

import br.com.cardapio.application.port.out.device.DevicePairingPort;
import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.DevicePairing;
import br.com.cardapio.domain.device.model.PairingCode;
import br.com.cardapio.domain.device.model.PairingId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class DevicePairingPersistenceAdapter implements DevicePairingPort {

    private final DevicePairingJpaRepository pairings;

    DevicePairingPersistenceAdapter(DevicePairingJpaRepository pairings) {
        this.pairings = pairings;
    }

    @Override
    public void save(DevicePairing pairing) {
        pairings.save(DevicePersistenceMapper.toEntity(pairing));
    }

    @Override
    public Optional<DevicePairing> findById(PairingId id) {
        return pairings.findById(id.value()).map(DevicePersistenceMapper::toDomain);
    }

    @Override
    public Optional<DevicePairing> findOpenByCode(PairingCode code, Instant now) {
        return pairings.findFirstByCodeAndDeviceIdIsNullAndExpiresAtAfterOrderByCreatedAtDesc(code.value(), now)
                .map(DevicePersistenceMapper::toDomain);
    }

    @Override
    public boolean linkIfOpen(PairingId id, DeviceId deviceId, Instant now) {
        return pairings.linkIfOpen(id.value(), deviceId.value(), now) == 1;
    }

    @Override
    public boolean claimIfLinked(PairingId id, Instant now) {
        return pairings.claimIfLinked(id.value(), now) == 1;
    }
}
