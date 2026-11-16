package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;
import java.util.Set;

/**
 * Os preços novos de um grupo de opções em todos os produtos: os preços antigos destas opções saem, e
 * entram estes.
 */
public record OptionPriceChange(EstablishmentId establishmentId, Set<OptionItemId> optionIds, List<Price> prices) {

    /** @param variantId null: o preço da opção neste produto (sem tamanhos) */
    public record Price(ProductId productId, ProductVariantId variantId, OptionItemId optionId, Money price) {
    }

    public OptionPriceChange {
        optionIds = Set.copyOf(optionIds);
        prices = List.copyOf(prices);
    }
}
