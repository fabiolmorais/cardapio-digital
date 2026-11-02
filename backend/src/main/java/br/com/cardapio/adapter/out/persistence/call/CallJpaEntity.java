package br.com.cardapio.adapter.out.persistence.call;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O chamado no banco. Não há construtor para gravar: a inserção é um INSERT nativo (ON CONFLICT)
 * e as mudanças são UPDATEs condicionais, os dois no repositório. A entidade serve para ler.
 */
@Entity
@Table(name = "call")
class CallJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "table_id", nullable = false)
    private UUID tableId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private String type;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by")
    private UUID acceptedBy;

    @Column(name = "done_at")
    private Instant doneAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    protected CallJpaEntity() {
    }

    UUID getId() { return id; }
    UUID getTableId() { return tableId; }
    UUID getSessionId() { return sessionId; }
    String getType() { return type; }
    String getPaymentMethod() { return paymentMethod; }
    String getStatus() { return status; }
    Instant getCreatedAt() { return createdAt; }
    Instant getAcceptedAt() { return acceptedAt; }
    UUID getAcceptedBy() { return acceptedBy; }
    Instant getDoneAt() { return doneAt; }
    Instant getCanceledAt() { return canceledAt; }
}
