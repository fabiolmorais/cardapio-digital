package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_order")
class OrderJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "tab_id", nullable = false)
    private UUID tabId;

    @Column(name = "table_id", nullable = false)
    private UUID tableId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected OrderJpaEntity() {
    }

    OrderJpaEntity(UUID id, UUID establishmentId, UUID tabId, UUID tableId, UUID sessionId, int number,
                   String status, Instant createdAt, UUID approvedBy, Instant approvedAt) {
        super(establishmentId);
        this.id = id;
        this.tabId = tabId;
        this.tableId = tableId;
        this.sessionId = sessionId;
        this.number = number;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }

    UUID getId() { return id; }
    UUID getTabId() { return tabId; }
    UUID getTableId() { return tableId; }
    UUID getSessionId() { return sessionId; }
    int getNumber() { return number; }
    String getStatus() { return status; }
    Instant getCreatedAt() { return createdAt; }
    UUID getApprovedBy() { return approvedBy; }
    Instant getApprovedAt() { return approvedAt; }
}
