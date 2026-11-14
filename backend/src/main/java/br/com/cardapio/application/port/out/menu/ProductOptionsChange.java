package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ChoiceLimit;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;

/**
 * A configuração nova de um produto, como a gestão salva: os tamanhos (inclusive os que saíram,
 * desativados), os grupos ligados na ordem, os limites e os preços por tamanho.
 */
public record ProductOptionsChange(EstablishmentId establishmentId, ProductId productId,
                                   List<ProductVariant> variants, List<OptionGroupId> groups, List<Limit> limits,
                                   List<Price> prices) {

    public record Limit(OptionGroupId groupId, ProductVariantId variantId, ChoiceLimit limit) {
    }

    public record Price(OptionItemId optionId, ProductVariantId variantId, Money price) {
    }

    public ProductOptionsChange {
        variants = List.copyOf(variants);
        groups = List.copyOf(groups);
        limits = List.copyOf(limits);
        prices = List.copyOf(prices);
    }
}
