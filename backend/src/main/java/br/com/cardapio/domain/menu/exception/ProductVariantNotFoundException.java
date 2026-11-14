package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

public class ProductVariantNotFoundException extends NotFoundException {

    public ProductVariantNotFoundException() {
        super("Tamanho não encontrado");
    }
}
