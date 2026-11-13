package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.KitchenItemView;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;

import java.time.Instant;
import java.util.function.Function;

/**
 * Um item como a COZINHA e o garçom veem na fila: o que é, de qual mesa, de qual pedido e há quanto
 * tempo. `photoUrl` é a miniatura da capa do produto (a "visão por item" da cozinha, 9.3), ou nulo.
 */
public record KitchenItem(OrderItemId id, OrderId orderId, int orderNumber, String tableLabel, String sectorName,
                          ProductId productId, String productName, String photoUrl, int quantity, String note,
                          OrderItemStatus status, Instant createdAt, Instant preparingAt, Instant readyAt) {

    public static KitchenItem from(KitchenItemView view) {
        return from(view, product -> null);
    }

    /** Com a miniatura de cada produto (`photos` devolve nulo para quem não tem foto). */
    public static KitchenItem from(KitchenItemView view, Function<ProductId, String> photos) {
        var i = view.item();
        return new KitchenItem(i.id(), i.orderId(), view.orderNumber(), view.tableLabel(), view.sectorName(),
                i.productId(), i.productName(), photos.apply(i.productId()), i.quantity(), i.note(), i.status(),
                i.createdAt(), i.preparingAt(), i.readyAt());
    }
}
