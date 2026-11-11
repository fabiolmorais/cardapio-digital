package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

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

    /**
     * @param cancelReason o motivo, quando cancelado ("acabou o salmão")
     * @param rodizio      veio numa rodada do rodízio (a tela mostra "Rodízio" em vez de R$ 0,00)
     * @param thumbUrl     a miniatura da foto do produto; null se ele não tem foto
     */
    public record Item(OrderItemId id, String productName, Money unitPrice, int quantity, String note,
                       OrderItemStatus status, String cancelReason, boolean rodizio, String thumbUrl) {

        static Item from(OrderItem i) {
            return from(i, product -> null);
        }

        static Item from(OrderItem i, Function<ProductId, String> thumbs) {
            return new Item(i.id(), i.productName(), i.unitPrice(), i.quantity(), i.note(), i.status(),
                    i.cancelReason(), i.inRodizio(), thumbs.apply(i.productId()));
        }
    }

    public static TableOrder from(OrderWithItems found, TableSessionId viewer) {
        return from(found, viewer, product -> null);
    }

    /** @param thumbs a miniatura de cada produto (null se não tem foto) */
    public static TableOrder from(OrderWithItems found, TableSessionId viewer, Function<ProductId, String> thumbs) {
        var order = found.order();
        return new TableOrder(order.id(), order.number(), order.status(), order.createdAt(),
                order.sessionId().equals(viewer), found.items().stream().map(i -> Item.from(i, thumbs)).toList());
    }
}
