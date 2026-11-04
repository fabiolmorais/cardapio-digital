package br.com.cardapio.domain.rodizio.model;

import java.util.Objects;
import java.util.UUID;

public record TabRodizioId(UUID value) {

    public TabRodizioId {
        Objects.requireNonNull(value);
    }

    public static TabRodizioId newId() {
        return new TabRodizioId(UUID.randomUUID());
    }
}
