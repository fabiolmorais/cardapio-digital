package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o pedido não está esperando a confirmação (já foi para a cozinha, ou já foi recusado). */
public class OrderStateException extends ConflictException {

    public OrderStateException(String message) {
        super(message);
    }
}
