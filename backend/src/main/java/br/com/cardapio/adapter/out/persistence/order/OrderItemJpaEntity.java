package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_item")
class OrderItemJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private int position;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(nullable = false)
    private int quantity;

    private String note;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "preparing_at")
    private Instant preparingAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivered_by")
    private UUID deliveredBy;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "tab_rodizio_id")
    private UUID tabRodizioId;

    /** Quando saiu na impressora da cozinha (a comanda de papel). */
    @Column(name = "printed_at")
    private Instant printedAt;

    /** Quando saiu a via "CANCELADO" do item já impresso. */
    @Column(name = "cancel_printed_at")
    private Instant cancelPrintedAt;

    protected OrderItemJpaEntity() {
    }

    OrderItemJpaEntity(UUID id, UUID establishmentId, UUID orderId, int position, UUID productId, String productName,
                       long unitPriceCents, int quantity, String note, String status, Instant createdAt,
                       UUID tabRodizioId) {
        super(establishmentId);
        this.id = id;
        this.orderId = orderId;
        this.position = position;
        this.productId = productId;
        this.productName = productName;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
        this.note = note;
        this.status = status;
        this.createdAt = createdAt;
        this.tabRodizioId = tabRodizioId;
    }

    UUID getId() { return id; }
    UUID getOrderId() { return orderId; }
    UUID getProductId() { return productId; }
    String getProductName() { return productName; }
    long getUnitPriceCents() { return unitPriceCents; }
    int getQuantity() { return quantity; }
    String getNote() { return note; }
    String getStatus() { return status; }
    Instant getCreatedAt() { return createdAt; }
    Instant getPreparingAt() { return preparingAt; }
    Instant getReadyAt() { return readyAt; }
    Instant getDeliveredAt() { return deliveredAt; }
    UUID getDeliveredBy() { return deliveredBy; }
    Instant getCanceledAt() { return canceledAt; }
    UUID getCanceledBy() { return canceledBy; }
    String getCancelReason() { return cancelReason; }
    UUID getTabRodizioId() { return tabRodizioId; }
    Instant getPrintedAt() { return printedAt; }
    Instant getCancelPrintedAt() { return cancelPrintedAt; }
}
