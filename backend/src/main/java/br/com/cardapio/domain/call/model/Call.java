package br.com.cardapio.domain.call.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.Objects;

/**
 * Um chamado da mesa. Cada mudança de estado guarda a hora (e, no aceite, quem aceitou), como
 * pede a especificação: é daqui que saem o tempo de espera e o de atendimento nos relatórios.
 *
 * @param paymentMethod só no pedido de conta (BILL); null no "chamar garçom"
 * @param acceptedBy    quem aceitou; null enquanto ninguém aceitou
 */
public record Call(
        CallId id,
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TableSessionId sessionId,
        CallType type,
        PaymentMethod paymentMethod,
        CallStatus status,
        Instant createdAt,
        Instant acceptedAt,
        UserId acceptedBy,
        Instant doneAt,
        Instant canceledAt
) {

    public Call {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        if (type == CallType.BILL && paymentMethod == null) {
            throw new InvalidValueException("Escolha como vai pagar: Pix, cartão ou dinheiro");
        }
        if (type != CallType.BILL) {
            paymentMethod = null; // só a conta tem forma de pagamento
        }
    }

    public static Call open(EstablishmentId establishmentId, DiningTableId tableId, TableSessionId sessionId,
                            CallType type, PaymentMethod paymentMethod, Instant now) {
        return new Call(CallId.newId(), establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.PENDING,
                now, null, null, null, null);
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    /** "Trocar forma de pagamento": a mesa muda de ideia enquanto a conta ainda não foi atendida. */
    public Call withPaymentMethod(PaymentMethod newMethod) {
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, Objects.requireNonNull(newMethod), status,
                createdAt, acceptedAt, acceptedBy, doneAt, canceledAt);
    }

    /** "Toque para atender": só um garçom fica com o chamado. */
    public Call accept(UserId by, Instant now) {
        if (status == CallStatus.IN_PROGRESS) {
            throw new CallStateException("Outra pessoa já aceitou este chamado");
        }
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.IN_PROGRESS,
                createdAt, now, Objects.requireNonNull(by), doneAt, canceledAt);
    }

    /** "Concluir": o garçom foi até a mesa. Só depois de aceitar (alguém precisa ter ficado com ele). */
    public Call finish(Instant now) {
        if (status != CallStatus.IN_PROGRESS) {
            throw new CallStateException(isOpen()
                    ? "Aceite o chamado antes de concluir"
                    : "Este chamado já foi encerrado");
        }
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.DONE,
                createdAt, acceptedAt, acceptedBy, now, canceledAt);
    }

    /** A mesa desistiu (antes ou depois de alguém aceitar). */
    public Call cancel(Instant now) {
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.CANCELED,
                createdAt, acceptedAt, acceptedBy, doneAt, now);
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new CallStateException("Este chamado já foi encerrado");
        }
    }
}
