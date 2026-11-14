package br.com.cardapio.domain.menu.model;

import java.util.Objects;
import java.util.UUID;

public record OptionGroupId(UUID value) {

    public OptionGroupId {
        Objects.requireNonNull(value, "id do grupo de opções é obrigatório");
    }

    public static OptionGroupId newId() {
        return new OptionGroupId(UUID.randomUUID());
    }
}
