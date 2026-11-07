package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;

import java.util.List;

/** Uma comanda: o pedido, a mesa, o setor e os itens que vão nela, na ordem do carrinho. */
public record PrintJobView(OrderId orderId, int orderNumber, DiningTableId tableId, String tableLabel,
                           String sectorName, List<OrderItem> items) {

    public PrintJobView {
        items = List.copyOf(items);
    }
}
