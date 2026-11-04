package br.com.cardapio.domain.rodizio.model;

import java.util.Objects;
import java.util.UUID;

public record RodizioId(UUID value) {

    public RodizioId {
        Objects.requireNonNull(value);
    }

    public static RodizioId newId() {
        return new RodizioId(UUID.randomUUID());
    }
}
