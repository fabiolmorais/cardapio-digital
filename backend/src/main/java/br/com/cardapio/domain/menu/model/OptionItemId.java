package br.com.cardapio.domain.menu.model;

import java.util.Objects;
import java.util.UUID;

public record OptionItemId(UUID value) implements MediaOwner {

    public OptionItemId {
        Objects.requireNonNull(value, "id da opção é obrigatório");
    }

    public static OptionItemId newId() {
        return new OptionItemId(UUID.randomUUID());
    }

    @Override
    public String folder() {
        return "options";
    }
}
