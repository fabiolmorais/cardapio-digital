package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.WaitingOrderView;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;

/**
 * Um pedido esperando o garçom: de qual mesa, quanto dá e o que tem. Os itens que o próprio
 * cliente já cancelou não entram no total.
 */
public record WaitingOrder(OrderId id, int number, String tableLabel, String sectorName, Instant createdAt,
                           Money total, List<TableOrder.Item> items) {

    public WaitingOrder {
        items = List.copyOf(items);
    }

    public static WaitingOrder from(WaitingOrderView view) {
        var order = view.order();
        Money total = view.items().stream()
                .filter(i -> i.status() != OrderItemStatus.CANCELED)
                .map(OrderItem::subtotal)
                .reduce(Money.ZERO, Money::plus);
        return new WaitingOrder(order.id(), order.number(), view.tableLabel(), view.sectorName(), order.createdAt(),
                total, view.items().stream().map(TableOrder.Item::from).toList());
    }
}
