package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/** Uma opção como o celular manda: qual e quantas vezes ("2× bacon"). O preço não vem do celular. */
public record SelectedOption(OptionItemId optionId, int quantity) {

    public SelectedOption {
        Objects.requireNonNull(optionId);
        if (quantity < 1 || quantity > OptionGroupRules.MAX_CHOICES) {
            throw new InvalidValueException("Cada opção vai de 1 a " + OptionGroupRules.MAX_CHOICES + " vezes");
        }
    }
}
