package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Pediu o PDF de um setor sem mesas ativas (422). */
public class NothingToPrintException extends DomainException {

    public NothingToPrintException() {
        super("Este setor não tem mesas ativas para imprimir.");
    }
}
