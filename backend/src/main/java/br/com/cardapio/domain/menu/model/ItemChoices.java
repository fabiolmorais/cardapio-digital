package br.com.cardapio.domain.menu.model;

import java.util.List;
import java.util.Objects;

/**
 * O tamanho e as escolhas de um item do pedido, copiados na hora (ADR 0010). Produto sem opções
 * fica com {@link #NONE}.
 *
 * @param variantId   o tamanho escolhido; null sem tamanhos
 * @param variantName o nome do tamanho copiado ("Grande"); null sem tamanhos
 * @param options     as escolhas, na ordem dos grupos e das opções
 */
public record ItemChoices(ProductVariantId variantId, String variantName, List<ChosenOption> options) {

    public static final ItemChoices NONE = new ItemChoices(null, null, List.of());

    public ItemChoices {
        options = List.copyOf(Objects.requireNonNull(options));
        if ((variantId == null) != (variantName == null)) {
            throw new IllegalArgumentException("o tamanho vem com o id e o nome");
        }
    }

    public boolean isEmpty() {
        return variantId == null && options.isEmpty();
    }

    /** As mesmas escolhas, com outras opções (ao ler do banco, que guarda o tamanho e as opções à parte). */
    public ItemChoices withOptions(List<ChosenOption> newOptions) {
        return new ItemChoices(variantId, variantName, newOptions);
    }
}
