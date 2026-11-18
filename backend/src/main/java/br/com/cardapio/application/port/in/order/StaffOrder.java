package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.TabId;

/** O pedido que a equipe acabou de lançar. */
public record StaffOrder(OrderId id, int number, TabId tabId) {

    public static StaffOrder from(Order order) {
        return new StaffOrder(order.id(), order.number(), order.tabId());
    }
}
