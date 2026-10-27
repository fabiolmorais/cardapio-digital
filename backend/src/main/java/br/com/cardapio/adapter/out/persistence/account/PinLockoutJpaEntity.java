package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pin_lockout")
class PinLockoutJpaEntity extends EstablishmentScopedEntity {

    @Id
    @Column(name = "membership_id")
    private UUID membershipId;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected PinLockoutJpaEntity() {
    }

    PinLockoutJpaEntity(UUID membershipId, UUID establishmentId, int failedAttempts, Instant lockedUntil) {
        super(establishmentId);
        this.membershipId = membershipId;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
    }

    UUID getMembershipId() { return membershipId; }
    int getFailedAttempts() { return failedAttempts; }
    Instant getLockedUntil() { return lockedUntil; }
}
