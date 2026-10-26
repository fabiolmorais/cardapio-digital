package br.com.cardapio.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "establishment_id", nullable = false)
    private UUID establishmentId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshTokenJpaEntity() {
    }

    RefreshTokenJpaEntity(UUID id, UUID familyId, UUID userId, UUID establishmentId, String tokenHash,
                          Instant createdAt, Instant expiresAt, Instant revokedAt) {
        this.id = id;
        this.familyId = familyId;
        this.userId = userId;
        this.establishmentId = establishmentId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    UUID getId() { return id; }
    UUID getFamilyId() { return familyId; }
    UUID getUserId() { return userId; }
    UUID getEstablishmentId() { return establishmentId; }
    String getTokenHash() { return tokenHash; }
    Instant getCreatedAt() { return createdAt; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getRevokedAt() { return revokedAt; }
}
