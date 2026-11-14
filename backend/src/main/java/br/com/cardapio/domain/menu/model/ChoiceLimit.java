package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/** O mínimo e o máximo de um grupo num tamanho do produto (broto: 1 sabor; grande: até 3). */
public record ChoiceLimit(int minChoices, int maxChoices) {

    public ChoiceLimit {
        if (maxChoices < 1 || maxChoices > OptionGroupRules.MAX_CHOICES) {
            throw new InvalidValueException("O máximo de escolhas vai de 1 a " + OptionGroupRules.MAX_CHOICES);
        }
        if (minChoices < 0 || minChoices > maxChoices) {
            throw new InvalidValueException("O mínimo de escolhas vai de 0 ao máximo (" + maxChoices + ")");
        }
    }
}
