package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * O que o dono preenche no formulário do produto: nome, descrição, preços e destaque.
 * As regras de preço moram aqui, e valem para criar e para editar.
 *
 * @param promoPrice null quando o produto não está em promoção
 */
public record ProductDetails(
        String name,
        String description,
        Money price,
        Money promoPrice,
        boolean featured
) {

    public static final int NAME_MAX = 80;
    public static final int DESCRIPTION_MAX = 300;
    /** R$ 99.999,99: acima disso é quase certo que alguém digitou errado. */
    public static final Money PRICE_MAX = Money.ofCents(9_999_999);

    public ProductDetails {
        name = validName(name);
        description = validDescription(description);
        Objects.requireNonNull(price, "preço é obrigatório");
        if (price.compareTo(PRICE_MAX) > 0) {
            throw new InvalidValueException("Preço alto demais (máximo R$ 99.999,99)");
        }
        if (promoPrice != null) {
            if (promoPrice.equals(Money.ZERO)) {
                throw new InvalidValueException("O preço promocional precisa ser maior que zero");
            }
            if (promoPrice.compareTo(price) >= 0) {
                throw new InvalidValueException("O preço promocional precisa ser menor que o preço normal");
            }
        }
    }

    public boolean onSale() {
        return promoPrice != null;
    }

    /** O que o cliente paga hoje: o preço promocional, se houver. */
    public Money currentPrice() {
        return onSale() ? promoPrice : price;
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Dê um nome ao produto");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX) {
            throw new InvalidValueException("Nome do produto longo demais (máximo " + NAME_MAX + " caracteres)");
        }
        return trimmed;
    }

    /** A descrição é opcional: sem ela, fica vazia (e não nula). */
    private static String validDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.length() > DESCRIPTION_MAX) {
            throw new InvalidValueException("Descrição longa demais (máximo " + DESCRIPTION_MAX + " caracteres)");
        }
        return trimmed;
    }
}
