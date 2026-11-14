package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.ChoiceLimit;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;
import java.util.Objects;

/**
 * A configuração de um produto, como a gestão salva: os tamanhos, os grupos ligados (na ordem em
 * que o cliente vê) e, quando houver, os limites e os preços por tamanho. Os tamanhos novos ainda não
 * têm id: os limites e os preços apontam para eles pela posição na lista.
 */
public record ProductOptionsForm(List<Variant> variants, List<Group> groups, List<Price> prices) {

    /** @param id null para um tamanho novo; o id de um tamanho que já existe para editá-lo */
    public record Variant(ProductVariantId id, String name, Money price, Money promoPrice, boolean available,
                          boolean rodizioDefault) {
    }

    /** Um grupo ligado, com o mínimo e o máximo em cada tamanho (quando mudam). */
    public record Group(OptionGroupId groupId, List<Limit> limits) {

        public Group {
            limits = List.copyOf(Objects.requireNonNull(limits));
        }
    }

    /** @param variant a posição do tamanho na lista {@code variants} */
    public record Limit(int variant, ChoiceLimit limit) {
    }

    /** O preço de uma opção num tamanho. @param variant a posição do tamanho na lista {@code variants} */
    public record Price(OptionItemId optionId, int variant, Money price) {
    }

    public ProductOptionsForm {
        variants = List.copyOf(Objects.requireNonNull(variants));
        groups = List.copyOf(Objects.requireNonNull(groups));
        prices = List.copyOf(Objects.requireNonNull(prices));
    }
}
