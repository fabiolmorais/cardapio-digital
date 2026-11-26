package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.CounterGuest;
import br.com.cardapio.domain.order.model.OrderItem;

/**
 * O item com o que a cozinha precisa ver junto: o número do pedido, a mesa e o setor. Uma
 * consulta só, sem N+1.
 */
/** @param counter no balcão (ADR 0015), o cliente da comanda; null na mesa */
public record KitchenItemView(OrderItem item, int orderNumber, DiningTableId tableId, String tableLabel,
                              String sectorName, CounterGuest counter) {

    /** Na mesa: sem o cliente do balcão. */
    public KitchenItemView(OrderItem item, int orderNumber, DiningTableId tableId, String tableLabel,
                           String sectorName) {
        this(item, orderNumber, tableId, tableLabel, sectorName, null);
    }
}
