package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o restaurante desligou o pedido pelo celular (só chamados e cardápio). */
public class OrderingDisabledException extends ConflictException {

    public OrderingDisabledException() {
        super("O restaurante não está recebendo pedidos pelo celular agora. Chame o garçom para pedir.");
    }
}
