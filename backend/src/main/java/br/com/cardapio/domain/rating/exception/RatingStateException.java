package br.com.cardapio.domain.rating.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: a avaliação não está no estado que a ação pede (ou o celular não pode avaliar agora). */
public class RatingStateException extends ConflictException {

    public RatingStateException(String message) {
        super(message);
    }
}
