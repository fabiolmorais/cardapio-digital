package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: a comanda não pode ir para esta mesa (a mesma, desativada, ou rodízios que não se juntam). */
public class TabMoveException extends DomainException {

    public TabMoveException(String message) {
        super(message);
    }
}
