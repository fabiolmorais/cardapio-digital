package br.com.cardapio.domain.account.model;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "id do usuário é obrigatório");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
