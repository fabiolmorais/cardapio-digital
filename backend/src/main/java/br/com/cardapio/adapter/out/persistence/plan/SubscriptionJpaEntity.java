package br.com.cardapio.adapter.out.persistence.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A assinatura é da empresa, não de um restaurante: não estende o EstablishmentScopedEntity.
 * O adaptador sempre chega nela pelo restaurante do usuário (o JOIN com establishment).
 */
@Entity
@Table(name = "subscription")
class SubscriptionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false)
    private String status;

    @Column(name = "billing_cycle")
    private String billingCycle;

    @Column(name = "trial_ends_at", nullable = false)
    private Instant trialEndsAt;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SubscriptionJpaEntity() {
    }

    SubscriptionJpaEntity(UUID id, UUID organizationId, UUID planId, String status, String billingCycle,
                          Instant trialEndsAt, Instant currentPeriodEnd, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.planId = planId;
        this.status = status;
        this.billingCycle = billingCycle;
        this.trialEndsAt = trialEndsAt;
        this.currentPeriodEnd = currentPeriodEnd;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getOrganizationId() { return organizationId; }
    UUID getPlanId() { return planId; }
    String getStatus() { return status; }
    String getBillingCycle() { return billingCycle; }
    Instant getTrialEndsAt() { return trialEndsAt; }
    Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    Instant getCreatedAt() { return createdAt; }
}
