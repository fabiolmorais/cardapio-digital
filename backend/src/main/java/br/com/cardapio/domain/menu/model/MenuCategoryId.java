package br.com.cardapio.domain.menu.model;

import java.util.Objects;
import java.util.UUID;

public record MenuCategoryId(UUID value) {

    public MenuCategoryId {
        Objects.requireNonNull(value, "id da categoria é obrigatório");
    }

    public static MenuCategoryId newId() {
        return new MenuCategoryId(UUID.randomUUID());
    }
}
