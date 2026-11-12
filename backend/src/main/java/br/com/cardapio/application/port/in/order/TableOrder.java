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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Um pedido como a MESA vê: todos os celulares da mesa veem todos os pedidos da comanda.
 *
 * @param mine  foi este celular que mandou (só ele cancela os itens)
 * @param phone de qual celular da mesa é o pedido, sem dizer quem: 1 é sempre o celular que está
 *              olhando; os outros, 2, 3… na ordem do primeiro pedido de cada um
 */
public record TableOrder(OrderId id, int number, OrderStatus status, Instant createdAt, boolean mine, int phone,
                         List<Item> items) {

    public TableOrder {
        items = List.copyOf(items);
    }

    /**
     * @param cancelReason o motivo, quando cancelado ("acabou o salmão")
     * @param rodizio      veio numa rodada do rodízio (a tela mostra "Rodízio" em vez de R$ 0,00)
     * @param thumbUrl     a miniatura da foto do produto; null se ele não tem foto
     * @param canceledByCustomer foi o próprio cliente que cancelou (e não a equipe)
     */
    public record Item(OrderItemId id, String productName, Money unitPrice, int quantity, String note,
                       OrderItemStatus status, String cancelReason, boolean rodizio, String thumbUrl,
                       boolean canceledByCustomer) {

        static Item from(OrderItem i) {
            return from(i, product -> null);
        }

        static Item from(OrderItem i, Function<ProductId, String> thumbs) {
            return new Item(i.id(), i.productName(), i.unitPrice(), i.quantity(), i.note(), i.status(),
                    i.cancelReason(), i.inRodizio(), thumbs.apply(i.productId()), i.canceledByCustomer());
        }
    }

    /** O pedido que este celular acabou de mandar: é dele, então o celular 1. */
    public static TableOrder from(OrderWithItems found, TableSessionId viewer) {
        return from(found, viewer, product -> null, session -> 1);
    }

    /**
     * @param thumbs a miniatura de cada produto (null se não tem foto)
     * @param phones o número de cada celular da mesa (ver {@link #phones})
     */
    public static TableOrder from(OrderWithItems found, TableSessionId viewer, Function<ProductId, String> thumbs,
                                  Function<TableSessionId, Integer> phones) {
        var order = found.order();
        return new TableOrder(order.id(), order.number(), order.status(), order.createdAt(),
                order.sessionId().equals(viewer), phones.apply(order.sessionId()),
                found.items().stream().map(i -> Item.from(i, thumbs)).toList());
    }

    /**
     * Numera os celulares que pediram: quem está olhando é o 1; os outros, 2, 3… na ordem do
     * primeiro pedido. É o "Celular 2" da tela: a mesa vê quanto cada um pediu sem saber quem é.
     *
     * @param orders os pedidos, do mais antigo ao mais novo
     */
    public static Map<TableSessionId, Integer> phones(List<OrderWithItems> orders, TableSessionId viewer) {
        Map<TableSessionId, Integer> numbers = new LinkedHashMap<>();
        numbers.put(viewer, 1);
        for (OrderWithItems o : orders) {
            numbers.putIfAbsent(o.order().sessionId(), numbers.size() + 1);
        }
        return numbers;
    }
}
