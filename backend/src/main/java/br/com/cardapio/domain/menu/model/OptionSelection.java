package br.com.cardapio.domain.menu.model;

import java.util.List;
import java.util.Objects;

/**
 * O que o cliente escolheu num item: o tamanho (se o produto tem tamanhos) e as opções.
 *
 * @param variantId null quando o produto não tem tamanhos (ou no rodízio, que usa o tamanho dele)
 */
public record OptionSelection(ProductVariantId variantId, List<SelectedOption> options) {

    public static final OptionSelection NONE = new OptionSelection(null, List.of());

    public OptionSelection {
        options = List.copyOf(Objects.requireNonNull(options));
    }
}
