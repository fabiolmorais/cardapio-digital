package br.com.cardapio.domain.account.model;

import java.util.Objects;

/**
 * O restaurante. É a unidade de separação de dados: tudo pertence a um estabelecimento.
 */
public record Establishment(
        EstablishmentId id,
        OrganizationId organizationId,
        String name,
        Slug slug,
        String timeZone,
        boolean active
) {

    public static final String DEFAULT_TIME_ZONE = "America/Sao_Paulo";

    public Establishment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(organizationId);
        Objects.requireNonNull(slug);
        Objects.requireNonNull(timeZone);
        name = Names.required(name, "Informe o nome do estabelecimento");
    }

    public static Establishment create(OrganizationId organizationId, String name, Slug slug) {
        return new Establishment(EstablishmentId.newId(), organizationId, name, slug, DEFAULT_TIME_ZONE, true);
    }
}
