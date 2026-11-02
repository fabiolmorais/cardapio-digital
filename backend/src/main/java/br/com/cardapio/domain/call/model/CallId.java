package br.com.cardapio.domain.call.model;

import java.util.Objects;
import java.util.UUID;

public record CallId(UUID value) {

    public CallId {
        Objects.requireNonNull(value, "id do chamado é obrigatório");
    }

    public static CallId newId() {
        return new CallId(UUID.randomUUID());
    }
}
