package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class TabNotFoundException extends NotFoundException {

    public TabNotFoundException() {
        super("Comanda não encontrada");
    }
}
