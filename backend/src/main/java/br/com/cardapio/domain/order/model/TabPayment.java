package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Um pagamento recebido pela comanda. A mesa pode pagar antes de fechar (pediu a conta com itens
 * ainda a caminho): o garçom confirma o valor e a forma, a comanda continua aberta e o que falta
 * pagar diminui. No fechamento, o que ainda faltava vira o último pagamento.
 *
 * @param callId o pedido de conta que o garçom atendeu ao receber; null no fechamento
 */
public record TabPayment(
        TabPaymentId id,
        EstablishmentId establishmentId,
        TabId tabId,
        Money amount,
        PaymentMethod method,
        Instant receivedAt,
        UserId receivedBy,
        CallId callId
) {

    public TabPayment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(method);
        Objects.requireNonNull(receivedAt);
        Objects.requireNonNull(receivedBy);
        if (amount.isZero()) {
            throw new InvalidValueException("O valor recebido precisa ser maior que zero");
        }
    }

    /**
     * O garçom recebe da mesa, com a comanda ainda aberta. Não recebe mais do que falta pagar: o
     * que passar disso seria troco, e não pagamento.
     */
    public static TabPayment receive(Tab tab, TabBalance balance, Money amount, PaymentMethod method, UserId by,
                                     CallId callId, Instant now) {
        if (!tab.status().isOpen()) {
            throw new TabStateException("Esta comanda já foi fechada");
        }
        Objects.requireNonNull(amount);
        if (method == null) {
            throw new InvalidValueException("Escolha como a mesa pagou: Pix, cartão ou dinheiro");
        }
        if (amount.isZero() || amount.compareTo(balance.due()) > 0) {
            throw new InvalidValueException("O valor recebido não pode passar do que falta pagar");
        }
        return new TabPayment(TabPaymentId.newId(), tab.establishmentId(), tab.id(), amount, method, now, by, callId);
    }
}
