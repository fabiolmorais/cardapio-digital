package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;

import java.util.List;

/** O pedido parado na confirmação, com a mesa, o setor e os itens (para o garçom decidir). */
public record WaitingOrderView(Order order, String tableLabel, String sectorName, List<OrderItem> items) {

    public WaitingOrderView {
        items = List.copyOf(items);
    }
}
