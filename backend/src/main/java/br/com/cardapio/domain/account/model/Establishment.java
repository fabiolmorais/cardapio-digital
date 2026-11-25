package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;
import java.util.Set;

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

    /**
     * Os fusos que uma unidade nova pode ter (os do Brasil hoje): Brasília, Amazonas, Mato Grosso,
     * Acre e Fernando de Noronha.
     */
    public static final Set<String> UNIT_TIME_ZONES = Set.of(DEFAULT_TIME_ZONE, "America/Manaus",
            "America/Cuiaba", "America/Rio_Branco", "America/Noronha");

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

    /**
     * Mais uma unidade da mesma empresa (RF31): um restaurante separado em tudo, que começa vazio.
     * A assinatura continua a da empresa.
     */
    public static Establishment unit(OrganizationId organizationId, String name, Slug slug, String timeZone) {
        if (timeZone == null || !UNIT_TIME_ZONES.contains(timeZone)) {
            throw new InvalidValueException("Escolha um fuso horário da lista");
        }
        return new Establishment(EstablishmentId.newId(), organizationId, name, slug, timeZone, true);
    }
}
