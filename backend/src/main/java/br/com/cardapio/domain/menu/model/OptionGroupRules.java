package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * Como um grupo de opções se comporta (ADR 0010). Poucos ajustes que se combinam cobrem os nichos:
 * o ponto da carne (1 escolha, obrigatória), os adicionais (até 5, somando, podendo repetir), a
 * pizza meio a meio (divide o item, cobra o maior preço) e o açaí (3 complementos grátis).
 *
 * @param minChoices   0 = opcional; 1 ou mais = obrigatório
 * @param maxChoices   quantas escolhas cabem (contando as repetições)
 * @param maxPerOption quantas vezes a mesma opção pode ser escolhida (1 = sem repetir)
 * @param splitsItem   as escolhas são partes do mesmo item (½, ⅓, ¼): no máximo 4, sem repetir
 * @param pricing      como as escolhas entram no preço
 * @param freeQuantity as primeiras N escolhas não cobram (só ao somar); ficam grátis as mais baratas
 */
public record OptionGroupRules(int minChoices, int maxChoices, int maxPerOption, boolean splitsItem,
                               OptionPricing pricing, int freeQuantity) {

    /** Escolhas num grupo. */
    public static final int MAX_CHOICES = 20;
    /** Partes de um item dividido (a pizza de 4 sabores). */
    public static final int MAX_PARTS = 4;

    public OptionGroupRules {
        Objects.requireNonNull(pricing, "diga como o grupo cobra");
        if (maxChoices < 1 || maxChoices > MAX_CHOICES) {
            throw new InvalidValueException("O máximo de escolhas vai de 1 a " + MAX_CHOICES);
        }
        if (minChoices < 0 || minChoices > maxChoices) {
            throw new InvalidValueException("O mínimo de escolhas vai de 0 ao máximo (" + maxChoices + ")");
        }
        if (maxPerOption < 1 || maxPerOption > maxChoices) {
            throw new InvalidValueException("Cada opção pode ser escolhida de 1 a " + maxChoices + " vezes");
        }
        if (splitsItem && (maxChoices > MAX_PARTS || maxPerOption != 1)) {
            throw new InvalidValueException("Um item dividido tem até " + MAX_PARTS
                    + " partes, cada uma de uma opção diferente");
        }
        if (freeQuantity < 0 || freeQuantity > maxChoices) {
            throw new InvalidValueException("As escolhas grátis vão de 0 ao máximo (" + maxChoices + ")");
        }
        if (freeQuantity > 0 && pricing != OptionPricing.SUM) {
            throw new InvalidValueException("Escolhas grátis só valem quando o grupo soma os preços");
        }
    }

    /** Este grupo, num tamanho com outro mínimo e máximo (broto: 1 sabor; grande: até 3). */
    public OptionGroupRules limitedTo(ChoiceLimit limit) {
        if (limit == null) {
            return this;
        }
        int max = limit.maxChoices();
        return new OptionGroupRules(limit.minChoices(), max, Math.min(maxPerOption, max), splitsItem, pricing,
                Math.min(freeQuantity, max));
    }

    public boolean required() {
        return minChoices > 0;
    }
}
