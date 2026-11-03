package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: a comanda não pode ser fechada agora (já fechada, ou com itens a caminho). */
public class TabStateException extends ConflictException {

    public TabStateException(String message) {
        super(message);
    }
}
