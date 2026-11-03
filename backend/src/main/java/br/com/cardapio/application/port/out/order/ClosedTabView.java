package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;

/**
 * Uma comanda fechada, como o histórico mostra: a mesa, quando abriu e fechou, quanto foi
 * cobrado, como pagou e quem fechou.
 *
 * @param closedByName o nome de quem fechou; null se a pessoa saiu da equipe e o cadastro sumiu
 */
public record ClosedTabView(TabId id, String tableLabel, String sectorName, Instant openedAt, Instant closedAt,
                            Money total, PaymentMethod paymentMethod, String closedByName) {
}
