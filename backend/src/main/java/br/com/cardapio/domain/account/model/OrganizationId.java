package br.com.cardapio.domain.account.model;

import java.util.Objects;
import java.util.UUID;

public record OrganizationId(UUID value) {

    public OrganizationId {
        Objects.requireNonNull(value, "id da organização é obrigatório");
    }

    public static OrganizationId newId() {
        return new OrganizationId(UUID.randomUUID());
    }
}
