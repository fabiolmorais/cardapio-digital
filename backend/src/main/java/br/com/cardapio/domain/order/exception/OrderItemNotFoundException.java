package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class OrderItemNotFoundException extends NotFoundException {

    public OrderItemNotFoundException() {
        super("Item do pedido não encontrado");
    }
}
