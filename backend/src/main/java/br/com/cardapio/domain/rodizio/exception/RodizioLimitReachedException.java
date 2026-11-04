package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o restaurante já tem o máximo de rodízios ativos. */
public class RodizioLimitReachedException extends DomainException {

    public RodizioLimitReachedException() {
        super("O restaurante pode ter até " + Rodizio.MAX_PER_ESTABLISHMENT
                + " rodízios ativos. Desative um antes de criar outro.");
    }
}
