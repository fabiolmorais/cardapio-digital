package br.com.cardapio.adapter.out.persistence.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Não herda EstablishmentScopedEntity: o pedido ainda não é de nenhum restaurante. */
@Entity
@Table(name = "device_pairing")
class DevicePairingJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected DevicePairingJpaEntity() {
    }

    DevicePairingJpaEntity(UUID id, String code, String secretHash, Instant createdAt, Instant expiresAt,
                           UUID deviceId, Instant claimedAt) {
        this.id = id;
        this.code = code;
        this.secretHash = secretHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.deviceId = deviceId;
        this.claimedAt = claimedAt;
    }

    UUID getId() { return id; }
    String getCode() { return code; }
    String getSecretHash() { return secretHash; }
    Instant getCreatedAt() { return createdAt; }
    Instant getExpiresAt() { return expiresAt; }
    UUID getDeviceId() { return deviceId; }
    Instant getClaimedAt() { return claimedAt; }
}
