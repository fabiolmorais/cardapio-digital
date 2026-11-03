package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException() {
        super("Pedido não encontrado");
    }
}
