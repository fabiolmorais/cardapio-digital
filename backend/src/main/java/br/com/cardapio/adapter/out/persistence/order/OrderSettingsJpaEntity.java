package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_settings")
class OrderSettingsJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "ordering_enabled", nullable = false)
    private boolean orderingEnabled;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "max_amount_cents")
    private Long maxAmountCents;

    @Column(name = "max_items")
    private Integer maxItems;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderSettingsJpaEntity() {
    }

    OrderSettingsJpaEntity(UUID id, UUID establishmentId, boolean orderingEnabled, boolean approvalRequired,
                           Long maxAmountCents, Integer maxItems, Instant updatedAt) {
        super(establishmentId);
        this.id = id;
        this.orderingEnabled = orderingEnabled;
        this.approvalRequired = approvalRequired;
        this.maxAmountCents = maxAmountCents;
        this.maxItems = maxItems;
        this.updatedAt = updatedAt;
    }

    UUID getId() { return id; }
    boolean isOrderingEnabled() { return orderingEnabled; }
    boolean isApprovalRequired() { return approvalRequired; }
    Long getMaxAmountCents() { return maxAmountCents; }
    Integer getMaxItems() { return maxItems; }
    Instant getUpdatedAt() { return updatedAt; }
}
