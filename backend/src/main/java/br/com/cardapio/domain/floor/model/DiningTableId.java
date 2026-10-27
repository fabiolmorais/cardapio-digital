package br.com.cardapio.domain.floor.model;

import java.util.Objects;
import java.util.UUID;

public record DiningTableId(UUID value) {

    public DiningTableId {
        Objects.requireNonNull(value, "id da mesa é obrigatório");
    }

    public static DiningTableId newId() {
        return new DiningTableId(UUID.randomUUID());
    }
}
