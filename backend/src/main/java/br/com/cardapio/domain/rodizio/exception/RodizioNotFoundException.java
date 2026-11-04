package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class RodizioNotFoundException extends NotFoundException {

    public RodizioNotFoundException() {
        super("Rodízio não encontrado");
    }
}
