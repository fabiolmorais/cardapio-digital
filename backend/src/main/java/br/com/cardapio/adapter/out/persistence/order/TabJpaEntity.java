package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A comanda no banco. A inserção é um INSERT nativo (ON CONFLICT), como a do chamado, e o
 * fechamento é um UPDATE condicional: a entidade serve para ler.
 */
@Entity
@Table(name = "tab")
class TabJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "table_id", nullable = false)
    private UUID tableId;

    @Column(nullable = false)
    private String status;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "total_cents")
    private Long totalCents;

    @Column(name = "payment_method")
    private String paymentMethod;

    protected TabJpaEntity() {
    }

    UUID getId() { return id; }
    UUID getTableId() { return tableId; }
    String getStatus() { return status; }
    Instant getOpenedAt() { return openedAt; }
    Instant getClosedAt() { return closedAt; }
    UUID getClosedBy() { return closedBy; }
    Long getTotalCents() { return totalCents; }
    String getPaymentMethod() { return paymentMethod; }
}
