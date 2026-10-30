package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException() {
        super("Produto não encontrado");
    }
}
