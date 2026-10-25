package br.com.cardapio.domain.account.model;

import java.util.Objects;
import java.util.UUID;

public record EstablishmentId(UUID value) {

    public EstablishmentId {
        Objects.requireNonNull(value, "id do estabelecimento é obrigatório");
    }

    public static EstablishmentId newId() {
        return new EstablishmentId(UUID.randomUUID());
    }
}
