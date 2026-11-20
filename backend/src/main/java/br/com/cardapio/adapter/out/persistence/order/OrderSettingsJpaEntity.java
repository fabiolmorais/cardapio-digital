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

    @Column(name = "staff_ordering_enabled", nullable = false)
    private boolean staffOrderingEnabled;

    @Column(name = "max_amount_cents")
    private Long maxAmountCents;

    @Column(name = "max_items")
    private Integer maxItems;

    @Column(name = "service_charge_enabled", nullable = false)
    private boolean serviceChargeEnabled;

    @Column(name = "service_charge_percent", nullable = false)
    private int serviceChargePercent;

    @Column(name = "service_charge_destination", nullable = false)
    private String serviceChargeDestination;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderSettingsJpaEntity() {
    }

    OrderSettingsJpaEntity(UUID id, UUID establishmentId, boolean orderingEnabled, boolean approvalRequired,
                           boolean staffOrderingEnabled, Long maxAmountCents, Integer maxItems,
                           boolean serviceChargeEnabled, int serviceChargePercent, String serviceChargeDestination,
                           Instant updatedAt) {
        super(establishmentId);
        this.id = id;
        this.orderingEnabled = orderingEnabled;
        this.approvalRequired = approvalRequired;
        this.staffOrderingEnabled = staffOrderingEnabled;
        this.maxAmountCents = maxAmountCents;
        this.maxItems = maxItems;
        this.serviceChargeEnabled = serviceChargeEnabled;
        this.serviceChargePercent = serviceChargePercent;
        this.serviceChargeDestination = serviceChargeDestination;
        this.updatedAt = updatedAt;
    }

    UUID getId() { return id; }
    boolean isOrderingEnabled() { return orderingEnabled; }
    boolean isApprovalRequired() { return approvalRequired; }
    boolean isStaffOrderingEnabled() { return staffOrderingEnabled; }
    Long getMaxAmountCents() { return maxAmountCents; }
    Integer getMaxItems() { return maxItems; }
    boolean isServiceChargeEnabled() { return serviceChargeEnabled; }
    int getServiceChargePercent() { return serviceChargePercent; }
    String getServiceChargeDestination() { return serviceChargeDestination; }
    Instant getUpdatedAt() { return updatedAt; }
}
