package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "membership")
class MembershipJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String role;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(nullable = false)
    private boolean active;

    protected MembershipJpaEntity() {
    }

    MembershipJpaEntity(UUID id, UUID userId, UUID establishmentId, String role, String pinHash, boolean active) {
        super(establishmentId);
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.pinHash = pinHash;
        this.active = active;
    }

    UUID getId() { return id; }
}
