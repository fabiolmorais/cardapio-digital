package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;

/**
 * A conta que a mesa pediu, para o garçom confirmar o que recebeu: o consumo, o que já foi pago e
 * o que falta.
 *
 * @param tabId         a comanda aberta da mesa; null se ela ainda não pediu nada
 * @param requestedWith a forma que a mesa escolheu ao pedir a conta
 * @param itemsOnTheWay quantos itens ainda não chegaram à mesa (a comanda não fecha com eles)
 */
public record BillSummary(TabId tabId, Money total, Money paid, Money due, PaymentMethod requestedWith,
                          int itemsOnTheWay) {
}
