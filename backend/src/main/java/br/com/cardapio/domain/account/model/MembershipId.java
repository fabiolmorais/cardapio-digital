package br.com.cardapio.domain.account.model;

import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {

    public MembershipId {
        Objects.requireNonNull(value, "id da adesão é obrigatório");
    }

    public static MembershipId newId() {
        return new MembershipId(UUID.randomUUID());
    }
}
