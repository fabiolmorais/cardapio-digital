package br.com.cardapio.domain.menu.model;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "id do produto é obrigatório");
    }

    public static ProductId newId() {
        return new ProductId(UUID.randomUUID());
    }
}
