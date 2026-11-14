package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * Um tamanho do produto (ADR 0010): Broto e Grande, 300 ml e 500 ml, Meia e Inteira, Dose e
 * Garrafa. Cada um tem preço, promoção e "esgotado" próprios. Nunca é apagado (os pedidos apontam
 * para ele): sai do cardápio desativado.
 *
 * @param promoPrice     null quando o tamanho não está em promoção
 * @param rodizioDefault é o tamanho que vale no rodízio (o rodízio é cobrado por pessoa)
 */
public record ProductVariant(
        ProductVariantId id,
        EstablishmentId establishmentId,
        ProductId productId,
        String name,
        Money price,
        Money promoPrice,
        boolean available,
        boolean rodizioDefault,
        int position,
        boolean active
) {

    public static final int NAME_MAX = 30;
    /** Tamanhos ativos num produto. */
    public static final int MAX_PER_PRODUCT = 10;

    public ProductVariant {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(productId);
        name = validName(name);
        Objects.requireNonNull(price, "preço do tamanho é obrigatório");
        if (price.compareTo(ProductDetails.PRICE_MAX) > 0) {
            throw new InvalidValueException("Preço alto demais (máximo R$ 99.999,99)");
        }
        if (promoPrice != null && (promoPrice.isZero() || promoPrice.compareTo(price) >= 0)) {
            throw new InvalidValueException("O preço promocional do tamanho " + name
                    + " precisa ser maior que zero e menor que o preço normal");
        }
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
    }

    /** O que o cliente paga hoje por este tamanho: o promocional, se houver. */
    public Money currentPrice() {
        return promoPrice != null ? promoPrice : price;
    }

    public ProductVariant withAvailability(boolean newAvailable) {
        return new ProductVariant(id, establishmentId, productId, name, price, promoPrice, newAvailable,
                rodizioDefault, position, active);
    }

    public ProductVariant deactivate() {
        return new ProductVariant(id, establishmentId, productId, name, price, promoPrice, available,
                rodizioDefault, position, false);
    }

    private static String validName(String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX) {
            throw new InvalidValueException("Dê um nome ao tamanho, com até " + NAME_MAX + " letras (ex.: Grande)");
        }
        return trimmed;
    }
}
