package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: já existe um rodízio ativo com este nome. */
public class RodizioNameTakenException extends ConflictException {

    public RodizioNameTakenException(String name) {
        super("Já existe um rodízio chamado \"" + name + "\"");
    }
}
