package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/** O preço de uma unidade do item, com as escolhas que entraram nele. */
public record PricedSelection(Money unitPrice, ItemChoices choices) {

    public PricedSelection {
        Objects.requireNonNull(unitPrice);
        Objects.requireNonNull(choices);
    }
}
