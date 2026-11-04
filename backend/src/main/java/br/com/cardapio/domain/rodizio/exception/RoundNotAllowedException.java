package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: a rodada esbarrou numa regra que o restaurante ligou (a mensagem diz qual, e o que fazer). */
public class RoundNotAllowedException extends DomainException {

    public RoundNotAllowedException(String message) {
        super(message);
    }
}
