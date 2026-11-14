package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * Uma opção de um grupo (Calabresa, Ao ponto, Bacon, Sem cebola), com o preço (pode ser zero) e o
 * "esgotado" do dia a dia. Nunca é apagada (os pedidos apontam para ela): sai desativada.
 */
public record OptionItem(OptionItemId id, EstablishmentId establishmentId, OptionGroupId groupId, String name,
                         Money price, boolean available, int position, boolean active) {

    public OptionItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(groupId);
        name = OptionGroup.text(name, "Dê um nome à opção");
        Objects.requireNonNull(price, "preço da opção é obrigatório");
        if (price.compareTo(ProductDetails.PRICE_MAX) > 0) {
            throw new InvalidValueException("Preço alto demais (máximo R$ 99.999,99)");
        }
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
    }

    public OptionItem withAvailability(boolean newAvailable) {
        return new OptionItem(id, establishmentId, groupId, name, price, newAvailable, position, active);
    }

    public OptionItem deactivate() {
        return new OptionItem(id, establishmentId, groupId, name, price, available, position, false);
    }
}
