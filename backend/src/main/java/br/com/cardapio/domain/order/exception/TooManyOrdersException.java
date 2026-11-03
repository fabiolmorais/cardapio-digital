package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 429: o mesmo celular mandou pedidos demais em pouco tempo. */
public class TooManyOrdersException extends DomainException {

    public TooManyOrdersException() {
        super("Muitos pedidos em pouco tempo. Espere um minuto e tente de novo.");
    }
}
