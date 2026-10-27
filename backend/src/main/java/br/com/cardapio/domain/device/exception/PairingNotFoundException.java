package br.com.cardapio.domain.device.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

/** Código errado, vencido ou já usado (404). As três situações têm a mesma resposta. */
public class PairingNotFoundException extends NotFoundException {

    public PairingNotFoundException() {
        super("Código não encontrado ou vencido. Gere um novo no aparelho.");
    }
}
