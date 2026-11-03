package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;

import java.util.List;

/** Um pedido com os itens dele, como as telas mostram. */
public record OrderWithItems(Order order, List<OrderItem> items) {

    public OrderWithItems {
        items = List.copyOf(items);
    }
}
