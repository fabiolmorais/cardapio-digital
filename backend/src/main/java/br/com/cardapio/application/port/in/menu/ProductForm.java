package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * O formulário do produto, igual para criar e para editar.
 *
 * @param promoPriceCents null quando não há promoção
 */
public record ProductForm(
        MenuCategoryId categoryId,
        String name,
        String description,
        long priceCents,
        Long promoPriceCents,
        boolean featured,
        boolean available
) {

    public ProductForm {
        Objects.requireNonNull(categoryId);
    }

    /** Monta (e valida) a parte do formulário que o domínio chama de detalhes. */
    public ProductDetails details() {
        return new ProductDetails(name, description, Money.ofCents(priceCents),
                promoPriceCents == null ? null : Money.ofCents(promoPriceCents), featured);
    }
}
