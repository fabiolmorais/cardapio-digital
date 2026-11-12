package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Um pagamento recebido pela comanda. Só é inserido, nunca muda. */
@Entity
@Table(name = "tab_payment")
class TabPaymentJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "tab_id", nullable = false)
    private UUID tabId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "received_by", nullable = false)
    private UUID receivedBy;

    @Column(name = "call_id")
    private UUID callId;

    protected TabPaymentJpaEntity() {
    }

    TabPaymentJpaEntity(UUID id, UUID establishmentId, UUID tabId, long amountCents, String paymentMethod,
                        Instant receivedAt, UUID receivedBy, UUID callId) {
        super(establishmentId);
        this.id = id;
        this.tabId = tabId;
        this.amountCents = amountCents;
        this.paymentMethod = paymentMethod;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
        this.callId = callId;
    }

    UUID getId() { return id; }
    UUID getTabId() { return tabId; }
    long getAmountCents() { return amountCents; }
    String getPaymentMethod() { return paymentMethod; }
    Instant getReceivedAt() { return receivedAt; }
    UUID getReceivedBy() { return receivedBy; }
    UUID getCallId() { return callId; }
}
