package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.KitchenItemView;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;

import java.time.Instant;

/** Um item como a COZINHA e o garçom veem na fila: o que é, de qual mesa, de qual pedido e há quanto tempo. */
public record KitchenItem(OrderItemId id, OrderId orderId, int orderNumber, String tableLabel, String sectorName,
                          String productName, int quantity, String note, OrderItemStatus status, Instant createdAt,
                          Instant preparingAt, Instant readyAt) {

    public static KitchenItem from(KitchenItemView view) {
        var i = view.item();
        return new KitchenItem(i.id(), i.orderId(), view.orderNumber(), view.tableLabel(), view.sectorName(),
                i.productName(), i.quantity(), i.note(), i.status(), i.createdAt(), i.preparingAt(), i.readyAt());
    }
}
