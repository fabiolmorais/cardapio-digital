package br.com.cardapio.domain.rodizio.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: a rodada trouxe um produto que não faz parte do rodízio (ele vai pelo carrinho, com preço). */
public class ProductNotInRodizioException extends ConflictException {

    public ProductNotInRodizioException(String productName) {
        super(productName + " não faz parte do rodízio. Peça pelo carrinho.");
    }
}
