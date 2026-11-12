package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o celular pediu uma rodada, mas ainda não está no rodízio da mesa (chegou depois da ativação). */
public class RodizioNotJoinedException extends ConflictException {

    public RodizioNotJoinedException() {
        super("Você ainda não está no rodízio desta mesa. Toque em \"Vou participar\" e o garçom inclui você.");
    }
}
