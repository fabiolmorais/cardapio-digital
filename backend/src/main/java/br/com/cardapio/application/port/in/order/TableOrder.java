package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.List;

/**
 * Um pedido como a MESA vê: todos os celulares da mesa veem todos os pedidos da comanda.
 *
 * @param mine foi este celular que mandou (só ele cancela os itens)
 */
public record TableOrder(OrderId id, int number, OrderStatus status, Instant createdAt, boolean mine,
                         List<Item> items) {

    public TableOrder {
        items = List.copyOf(items);
    }

    /** @param cancelReason o motivo, quando cancelado ("acabou o salmão") */
    public record Item(OrderItemId id, String productName, Money unitPrice, int quantity, String note,
                       OrderItemStatus status, String cancelReason) {

        static Item from(OrderItem i) {
            return new Item(i.id(), i.productName(), i.unitPrice(), i.quantity(), i.note(), i.status(),
                    i.cancelReason());
        }
    }

    public static TableOrder from(OrderWithItems found, TableSessionId viewer) {
        var order = found.order();
        return new TableOrder(order.id(), order.number(), order.status(), order.createdAt(),
                order.sessionId().equals(viewer), found.items().stream().map(Item::from).toList());
    }
}
