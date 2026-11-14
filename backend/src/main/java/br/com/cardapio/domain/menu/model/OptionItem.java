package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * Uma opção de um grupo (Calabresa, Ao ponto, Bacon, Sem cebola), com o preço (pode ser zero) e o
 * "esgotado" do dia a dia. Nunca é apagada (os pedidos apontam para ela): sai desativada.
 *
 * @param description os ingredientes do sabor, por exemplo; vazia se o restaurante não escreveu
 *                    (as fotos e os vídeos ficam na galeria da opção, como os do prato)
 */
public record OptionItem(OptionItemId id, EstablishmentId establishmentId, OptionGroupId groupId, String name,
                         String description, Money price, boolean available, int position, boolean active) {

    /** Do tamanho da descrição do prato. */
    public static final int DESCRIPTION_MAX = ProductDetails.DESCRIPTION_MAX;

    public OptionItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(groupId);
        name = OptionGroup.text(name, "Dê um nome à opção");
        description = description == null ? "" : description.strip();
        if (description.length() > DESCRIPTION_MAX) {
            throw new InvalidValueException("Descrição da opção longa demais (máximo " + DESCRIPTION_MAX
                    + " caracteres)");
        }
        Objects.requireNonNull(price, "preço da opção é obrigatório");
        if (price.compareTo(ProductDetails.PRICE_MAX) > 0) {
            throw new InvalidValueException("Preço alto demais (máximo R$ 99.999,99)");
        }
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
    }

    public OptionItem withAvailability(boolean newAvailable) {
        return new OptionItem(id, establishmentId, groupId, name, description, price, newAvailable, position, active);
    }

    public OptionItem deactivate() {
        return new OptionItem(id, establishmentId, groupId, name, description, price, available, position, false);
    }
}
