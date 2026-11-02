package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o cardápio chegou ao teto de produtos ativos. */
public class MenuFullException extends DomainException {

    public MenuFullException() {
        super("O cardápio pode ter até " + Product.MAX_PER_ESTABLISHMENT
                + " produtos. Desative algum que não é mais vendido antes de criar outro.");
    }
}
