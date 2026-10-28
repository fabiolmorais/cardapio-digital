package br.com.cardapio.domain.floor.model;

import java.util.Objects;
import java.util.UUID;

public record SectorId(UUID value) {

    public SectorId {
        Objects.requireNonNull(value, "id do setor é obrigatório");
    }

    public static SectorId newId() {
        return new SectorId(UUID.randomUUID());
    }
}
