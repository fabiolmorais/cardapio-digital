package br.com.cardapio.domain.menu.model;

import java.util.Objects;
import java.util.UUID;

public record ProductVariantId(UUID value) {

    public ProductVariantId {
        Objects.requireNonNull(value, "id do tamanho do produto é obrigatório");
    }

    public static ProductVariantId newId() {
        return new ProductVariantId(UUID.randomUUID());
    }
}
