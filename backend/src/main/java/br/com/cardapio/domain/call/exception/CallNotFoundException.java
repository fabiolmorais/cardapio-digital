package br.com.cardapio.domain.call.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class CallNotFoundException extends NotFoundException {

    public CallNotFoundException() {
        super("Chamado não encontrado");
    }
}
