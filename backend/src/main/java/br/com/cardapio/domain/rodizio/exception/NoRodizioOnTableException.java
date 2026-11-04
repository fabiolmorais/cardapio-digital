package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: a mesa pediu uma rodada (ou o garçom mexeu nas pessoas), mas o rodízio não foi ativado. */
public class NoRodizioOnTableException extends ConflictException {

    public NoRodizioOnTableException() {
        super("Esta mesa não está no rodízio. Chame o garçom para ativar.");
    }
}
