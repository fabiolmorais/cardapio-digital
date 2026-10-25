package br.com.cardapio.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "membership")
class MembershipJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "establishment_id", nullable = false)
    private UUID establishmentId;

    @Column(nullable = false)
    private String role;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(nullable = false)
    private boolean active;

    protected MembershipJpaEntity() {
    }

    MembershipJpaEntity(UUID id, UUID userId, UUID establishmentId, String role, boolean active) {
        this.id = id;
        this.userId = userId;
        this.establishmentId = establishmentId;
        this.role = role;
        this.active = active;
    }

    UUID getId() { return id; }
}
