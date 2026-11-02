package br.com.cardapio.domain.tablesession.model;

import java.util.Objects;
import java.util.UUID;

public record TableSessionId(UUID value) {

    public TableSessionId {
        Objects.requireNonNull(value, "id da sessão é obrigatório");
    }

    public static TableSessionId newId() {
        return new TableSessionId(UUID.randomUUID());
    }
}
