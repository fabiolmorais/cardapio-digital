package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o carrinho trouxe um produto que é só do rodízio (ele não tem preço à la carte). */
public class ProductOnlyInRodizioException extends ConflictException {

    public ProductOnlyInRodizioException(String productName) {
        super(productName + " é só do rodízio. Peça pela aba Rodízio.");
    }
}
