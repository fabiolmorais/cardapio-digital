package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.order.model.OrderItemId;

public interface CancelOrderItemUseCase {

    /** O cliente desiste de um item que ELE pediu, enquanto o item está na fila. */
    void cancel(String tableToken, String sessionToken, OrderItemId itemId);
}
