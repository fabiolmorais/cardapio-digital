package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * O formulário do produto, igual para criar e para editar.
 *
 * @param promoPriceCents null quando não há promoção
 * @param serves          quantas pessoas o prato serve; null quando o dono não informa
 */
public record ProductForm(
        MenuCategoryId categoryId,
        String name,
        String description,
        long priceCents,
        Long promoPriceCents,
        boolean featured,
        boolean available,
        boolean aLaCarte,
        Integer serves
) {

    public ProductForm {
        Objects.requireNonNull(categoryId);
    }

    /** O produto de sempre, que aparece no cardápio à la carte. */
    public ProductForm(MenuCategoryId categoryId, String name, String description, long priceCents,
                       Long promoPriceCents, boolean featured, boolean available) {
        this(categoryId, name, description, priceCents, promoPriceCents, featured, available, true, null);
    }

    /** Sem informar quantas pessoas o prato serve. */
    public ProductForm(MenuCategoryId categoryId, String name, String description, long priceCents,
                       Long promoPriceCents, boolean featured, boolean available, boolean aLaCarte) {
        this(categoryId, name, description, priceCents, promoPriceCents, featured, available, aLaCarte, null);
    }

    /** Monta (e valida) a parte do formulário que o domínio chama de detalhes. */
    public ProductDetails details() {
        return new ProductDetails(name, description, Money.ofCents(priceCents),
                promoPriceCents == null ? null : Money.ofCents(promoPriceCents), featured, aLaCarte, serves);
    }
}
