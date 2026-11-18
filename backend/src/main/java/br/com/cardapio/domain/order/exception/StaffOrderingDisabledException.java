package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o restaurante não liberou o lançamento de pedidos pela equipe. */
public class StaffOrderingDisabledException extends ConflictException {

    public StaffOrderingDisabledException() {
        super("O lançamento de pedidos pela equipe está desligado neste restaurante.");
    }
}
