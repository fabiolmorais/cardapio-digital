package br.com.cardapio.domain.call.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.exception.WrongReleaseCodeException;
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
 * @param releaseCode   só no pedido de liberação (UNLOCK): os 4 números que aparecem no celular de
 *                      quem pediu e que o garçom digita ao liberar. É a prova de que ele olhou o
 *                      celular na mesa: quem pede de casa não tem como mostrar a tela ao garçom.
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
        Instant canceledAt,
        String releaseCode
) {

    /**
     * Quantos chamados um mesmo celular pode ABRIR por minuto. Tocar de novo num chamado aberto não
     * conta (ele não duplica); o que este limite segura é abrir e cancelar sem parar.
     */
    public static final int MAX_NEW_PER_MINUTE = 5;

    /** Quantos números tem o código da liberação. */
    public static final int RELEASE_CODE_LENGTH = 4;

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
        if (type == CallType.UNLOCK) {
            // Os encerrados antes do código (até a V32) não têm; aberto, sempre tem.
            boolean valid = releaseCode != null && releaseCode.matches("\\d{" + RELEASE_CODE_LENGTH + "}");
            if (!valid && status.isOpen()) {
                throw new IllegalArgumentException("o pedido de liberação precisa de um código de 4 números");
            }
        } else {
            releaseCode = null; // só a liberação tem código
        }
    }

    public static Call open(EstablishmentId establishmentId, DiningTableId tableId, TableSessionId sessionId,
                            CallType type, PaymentMethod paymentMethod, Instant now) {
        return new Call(CallId.newId(), establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.PENDING,
                now, null, null, null, null, null);
    }

    /** "Pedir liberação ao garçom": o pedido nasce com o código que o celular de quem pediu mostra. */
    public static Call openUnlock(EstablishmentId establishmentId, DiningTableId tableId, TableSessionId sessionId,
                                  String releaseCode, Instant now) {
        return new Call(CallId.newId(), establishmentId, tableId, sessionId, CallType.UNLOCK, null,
                CallStatus.PENDING, now, null, null, null, null, releaseCode);
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    /** "Trocar forma de pagamento": a mesa muda de ideia enquanto a conta ainda não foi atendida. */
    public Call withPaymentMethod(PaymentMethod newMethod) {
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, Objects.requireNonNull(newMethod), status,
                createdAt, acceptedAt, acceptedBy, doneAt, canceledAt, releaseCode);
    }

    /** "Toque para atender": só um garçom fica com o chamado. A liberação usa o release. */
    public Call accept(UserId by, Instant now) {
        if (type == CallType.UNLOCK) {
            throw new CallStateException("Este pedido de liberação se responde com Liberar ou Recusar");
        }
        if (status == CallStatus.IN_PROGRESS) {
            throw new CallStateException("Outra pessoa já aceitou este chamado");
        }
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.IN_PROGRESS,
                createdAt, now, Objects.requireNonNull(by), doneAt, canceledAt, releaseCode);
    }

    /** "Concluir": o garçom foi até a mesa. Só depois de aceitar (alguém precisa ter ficado com ele). */
    public Call finish(Instant now) {
        if (status != CallStatus.IN_PROGRESS) {
            throw new CallStateException(isOpen()
                    ? "Aceite o chamado antes de concluir"
                    : "Este chamado já foi encerrado");
        }
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.DONE,
                createdAt, acceptedAt, acceptedBy, now, canceledAt, releaseCode);
    }

    /**
     * "Liberar": no pedido de liberação não há "estou indo" e depois "atendido". O garçom vai até a
     * mesa, digita o código que o celular de quem pediu mostra, e o chamado termina ali (aceito e
     * concluído juntos). Código errado: nada muda, e ele pode tentar de novo.
     */
    public Call release(UserId by, String typedCode, Instant now) {
        requirePendingUnlock();
        if (typedCode == null || !releaseCode.equals(typedCode.trim())) {
            throw new WrongReleaseCodeException();
        }
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.DONE,
                createdAt, now, Objects.requireNonNull(by), now, canceledAt, releaseCode);
    }

    /**
     * "Recusar": na liberação, o garçom não libera (o celular não está no salão); no "Quero o
     * rodízio", a mesa não vai entrar no rodízio (ou quem pediu não vai participar). O chamado é
     * cancelado. A liberação só se recusa antes de alguém responder; o rodízio, enquanto estiver aberto.
     */
    public Call decline(Instant now) {
        if (type == CallType.RODIZIO) {
            requireOpen();
        } else {
            requirePendingUnlock();
        }
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.CANCELED,
                createdAt, acceptedAt, acceptedBy, doneAt, now, releaseCode);
    }

    private void requirePendingUnlock() {
        if (type != CallType.UNLOCK) {
            throw new CallStateException("Só o pedido de liberação se libera ou recusa");
        }
        if (status != CallStatus.PENDING) {
            throw new CallStateException(isOpen() ? "Outra pessoa já aceitou este chamado"
                    : "Este chamado já foi encerrado");
        }
    }

    /**
     * A mesa entrou no rodízio: o "Quero o rodízio" termina atendido, por quem ativou (se ninguém
     * tinha aceitado antes, quem ativou conta como quem aceitou).
     */
    public Call resolveWithRodizio(UserId by, Instant now) {
        if (type != CallType.RODIZIO) {
            throw new CallStateException("Só o pedido de rodízio termina na ativação");
        }
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.DONE,
                createdAt, acceptedAt != null ? acceptedAt : now, acceptedBy != null ? acceptedBy : by, now,
                canceledAt, releaseCode);
    }

    /** A mesa desistiu (antes ou depois de alguém aceitar). */
    public Call cancel(Instant now) {
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.CANCELED,
                createdAt, acceptedAt, acceptedBy, doneAt, now, releaseCode);
    }

    /**
     * A comanda da mesa fechou: o chamado ainda aberto (a conta pedida, um "chamar garçom" que
     * ficou para trás) termina junto, como atendido.
     */
    public Call closeWithTab(Instant now) {
        requireOpen();
        return new Call(id, establishmentId, tableId, sessionId, type, paymentMethod, CallStatus.DONE,
                createdAt, acceptedAt, acceptedBy, now, canceledAt, releaseCode);
    }

    /** O código da liberação é um segredo de curta duração: nunca vai para o log. */
    @Override
    public String toString() {
        return "Call[id=" + id + ", tableId=" + tableId + ", type=" + type + ", status=" + status + "]";
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new CallStateException("Este chamado já foi encerrado");
        }
    }
}
