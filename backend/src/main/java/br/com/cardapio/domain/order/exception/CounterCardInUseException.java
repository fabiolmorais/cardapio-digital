package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o cartão já está numa comanda aberta do restaurante (ADR 0015). */
public class CounterCardInUseException extends ConflictException {

    public CounterCardInUseException(String card) {
        super("O cartão " + card + " já está em outra comanda aberta");
    }
}
