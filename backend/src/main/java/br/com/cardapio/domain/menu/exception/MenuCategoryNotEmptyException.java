package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Categoria com produtos ativos não pode ser desativada (422): os produtos ficariam sem aba. */
public class MenuCategoryNotEmptyException extends DomainException {

    public MenuCategoryNotEmptyException(long activeProducts) {
        super(activeProducts == 1
                ? "Esta categoria tem 1 produto. Mova ou desative o produto antes."
                : "Esta categoria tem " + activeProducts + " produtos. Mova ou desative os produtos antes.");
    }
}
