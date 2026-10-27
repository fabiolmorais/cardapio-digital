package br.com.cardapio.adapter.out.persistence.device;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_session")
class DeviceSessionJpaEntity extends EstablishmentScopedEntity {

    @Id
    @Column(name = "family_id")
    private UUID familyId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeviceSessionJpaEntity() {
    }

    DeviceSessionJpaEntity(UUID familyId, UUID deviceId, UUID establishmentId, Instant createdAt) {
        super(establishmentId);
        this.familyId = familyId;
        this.deviceId = deviceId;
        this.createdAt = createdAt;
    }
}
