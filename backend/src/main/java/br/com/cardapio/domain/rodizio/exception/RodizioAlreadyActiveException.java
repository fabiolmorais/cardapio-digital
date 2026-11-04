package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: a comanda já tem um rodízio (o garçom ajusta as pessoas, mas não troca de rodízio). */
public class RodizioAlreadyActiveException extends ConflictException {

    public RodizioAlreadyActiveException(String name) {
        super("Esta mesa já está no " + name + ".");
    }
}
