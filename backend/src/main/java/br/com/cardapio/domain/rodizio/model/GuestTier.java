package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * Quantas pessoas de uma faixa estão no rodízio, e quanto cada uma paga. O preço é COPIADO na
 * ativação (o do dia e horário de então): mudar o cadastro depois não muda a conta da mesa.
 */
public record GuestTier(String tierName, int quantity, Money unitPrice) {

    public static final int MAX_PER_TIER = 99;

    public GuestTier {
        Objects.requireNonNull(tierName);
        Objects.requireNonNull(unitPrice);
        if (quantity < 0 || quantity > MAX_PER_TIER) {
            throw new InvalidValueException("Cada faixa vai de 0 a " + MAX_PER_TIER + " pessoas.");
        }
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }

    GuestTier withQuantity(int newQuantity) {
        return new GuestTier(tierName, newQuantity, unitPrice);
    }
}
