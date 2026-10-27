package br.com.cardapio.adapter.out.persistence.device;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.DevicePairing;
import br.com.cardapio.domain.device.model.DeviceSecretHash;
import br.com.cardapio.domain.device.model.DeviceType;
import br.com.cardapio.domain.device.model.PairingCode;
import br.com.cardapio.domain.device.model.PairingId;

final class DevicePersistenceMapper {

    private DevicePersistenceMapper() {
    }

    static DeviceJpaEntity toEntity(Device d) {
        return new DeviceJpaEntity(d.id().value(), d.establishmentId().value(), d.name(), d.type().name(),
                d.tokenHash() == null ? null : d.tokenHash().value(), d.createdAt(), d.lastSeenAt(), d.revokedAt());
    }

    static Device toDomain(DeviceJpaEntity e) {
        return new Device(new DeviceId(e.getId()), new EstablishmentId(e.getEstablishmentId()), e.getName(),
                DeviceType.valueOf(e.getType()),
                e.getTokenHash() == null ? null : new DeviceSecretHash(e.getTokenHash()),
                e.getCreatedAt(), e.getLastSeenAt(), e.getRevokedAt());
    }

    static DevicePairingJpaEntity toEntity(DevicePairing p) {
        return new DevicePairingJpaEntity(p.id().value(), p.code().value(), p.secretHash().value(), p.createdAt(),
                p.expiresAt(), p.deviceId() == null ? null : p.deviceId().value(), p.claimedAt());
    }

    static DevicePairing toDomain(DevicePairingJpaEntity e) {
        return new DevicePairing(new PairingId(e.getId()), new PairingCode(e.getCode()),
                new DeviceSecretHash(e.getSecretHash()), e.getCreatedAt(), e.getExpiresAt(),
                e.getDeviceId() == null ? null : new DeviceId(e.getDeviceId()), e.getClaimedAt());
    }
}
