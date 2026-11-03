package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o item não pode fazer isso no status em que está (cancelar o que a cozinha já começou, por exemplo). */
public class OrderItemStateException extends ConflictException {

    public OrderItemStateException(String message) {
        super(message);
    }
}
