package br.com.cardapio.adapter.out.persistence.tablesession;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "table_session")
class TableSessionJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "table_id", nullable = false)
    private UUID tableId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected TableSessionJpaEntity() {
    }

    TableSessionJpaEntity(UUID id, UUID establishmentId, UUID tableId, String tokenHash, Instant createdAt,
                          Instant lastSeenAt, Instant expiresAt, Instant endedAt) {
        super(establishmentId);
        this.id = id;
        this.tableId = tableId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
        this.expiresAt = expiresAt;
        this.endedAt = endedAt;
    }

    UUID getId() { return id; }
    UUID getTableId() { return tableId; }
    String getTokenHash() { return tokenHash; }
    Instant getCreatedAt() { return createdAt; }
    Instant getLastSeenAt() { return lastSeenAt; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getEndedAt() { return endedAt; }
}
