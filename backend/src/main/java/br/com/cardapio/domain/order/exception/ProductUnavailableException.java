package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: um item do carrinho saiu do cardápio ou esgotou entre a escolha e o envio. */
public class ProductUnavailableException extends ConflictException {

    public ProductUnavailableException(String productName) {
        super(productName == null
                ? "Um item do carrinho não está mais no cardápio. Confira e envie de novo."
                : productName + " acabou de esgotar. Tire do carrinho e envie de novo.");
    }
}
