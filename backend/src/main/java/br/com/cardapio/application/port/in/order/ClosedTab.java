package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.ClosedTabView;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;

/** Uma comanda fechada no histórico: a mesa, quando abriu e fechou, quanto e como pagou, e quem fechou. */
public record ClosedTab(TabId id, String tableLabel, String sectorName, Instant openedAt, Instant closedAt,
                        Money total, PaymentMethod paymentMethod, String closedByName) {

    public static ClosedTab from(ClosedTabView view) {
        return new ClosedTab(view.id(), view.tableLabel(), view.sectorName(), view.openedAt(), view.closedAt(),
                view.total(), view.paymentMethod(), view.closedByName());
    }
}
