package br.com.cardapio.adapter.out.persistence.floor;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dining_table")
class DiningTableJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "sector_id", nullable = false)
    private UUID sectorId;

    @Column(nullable = false)
    private String label;

    @Column(name = "public_token", nullable = false)
    private String publicToken;

    @Column(name = "token_issued_at", nullable = false)
    private Instant tokenIssuedAt;

    @Column(nullable = false)
    private boolean active;

    protected DiningTableJpaEntity() {
    }

    DiningTableJpaEntity(UUID id, UUID establishmentId, UUID sectorId, String label, String publicToken,
                         Instant tokenIssuedAt, boolean active) {
        super(establishmentId);
        this.id = id;
        this.sectorId = sectorId;
        this.label = label;
        this.publicToken = publicToken;
        this.tokenIssuedAt = tokenIssuedAt;
        this.active = active;
    }

    UUID getId() { return id; }
    UUID getSectorId() { return sectorId; }
    String getLabel() { return label; }
    String getPublicToken() { return publicToken; }
    Instant getTokenIssuedAt() { return tokenIssuedAt; }
    boolean isActive() { return active; }
}
