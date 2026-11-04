package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o rodízio de passagem já se cadastra, mas só funciona na mesa na Etapa 12. */
public class RodizioNotServedException extends DomainException {

    public RodizioNotServedException() {
        super("O rodízio de passagem ainda não pode ser ativado na mesa.");
    }
}
