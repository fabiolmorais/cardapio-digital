package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class TableNotFoundException extends NotFoundException {

    public TableNotFoundException() {
        super("Mesa não encontrada");
    }
}
