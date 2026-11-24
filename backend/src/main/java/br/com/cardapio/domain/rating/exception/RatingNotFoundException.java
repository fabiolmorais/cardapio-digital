package br.com.cardapio.domain.rating.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

/** 404: não há avaliação com este id neste restaurante. */
public class RatingNotFoundException extends NotFoundException {

    public RatingNotFoundException() {
        super("Avaliação não encontrada");
    }
}
