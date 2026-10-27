package br.com.cardapio.adapter.out.persistence.device;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device")
class DeviceJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected DeviceJpaEntity() {
    }

    DeviceJpaEntity(UUID id, UUID establishmentId, String name, String type, String tokenHash,
                    Instant createdAt, Instant lastSeenAt, Instant revokedAt) {
        super(establishmentId);
        this.id = id;
        this.name = name;
        this.type = type;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
        this.revokedAt = revokedAt;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getType() { return type; }
    String getTokenHash() { return tokenHash; }
    Instant getCreatedAt() { return createdAt; }
    Instant getLastSeenAt() { return lastSeenAt; }
    Instant getRevokedAt() { return revokedAt; }
}
