package br.com.cardapio.adapter.out.persistence.push;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_subscription")
class PushSubscriptionJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PushSubscriptionJpaEntity() {
    }

    PushSubscriptionJpaEntity(UUID id, UUID establishmentId, UUID userId, String endpoint, String p256dh, String auth,
                              Instant createdAt) {
        super(establishmentId);
        this.id = id;
        this.userId = userId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getEndpoint() { return endpoint; }
    String getP256dh() { return p256dh; }
    String getAuth() { return auth; }
    Instant getCreatedAt() { return createdAt; }
}
