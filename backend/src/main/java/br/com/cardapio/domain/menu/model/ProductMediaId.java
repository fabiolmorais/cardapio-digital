package br.com.cardapio.domain.menu.model;

import java.util.Objects;
import java.util.UUID;

public record ProductMediaId(UUID value) {

    public ProductMediaId {
        Objects.requireNonNull(value, "id da mídia é obrigatório");
    }

    public static ProductMediaId newId() {
        return new ProductMediaId(UUID.randomUUID());
    }
}
