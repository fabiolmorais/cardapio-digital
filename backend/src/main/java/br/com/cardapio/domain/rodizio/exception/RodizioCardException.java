package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o cartão verde/vermelho é do rodízio de passagem (no à la minute, a mesa pede rodadas). */
public class RodizioCardException extends ConflictException {

    public RodizioCardException() {
        super("O cartão verde e vermelho é do rodízio de passagem.");
    }
}
