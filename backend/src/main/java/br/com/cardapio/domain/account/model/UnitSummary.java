package br.com.cardapio.domain.account.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Uma unidade da empresa na lista "Unidades" (RF31): o restaurante, quando foi criado e quanto
 * ele tem de mesas e de equipe ativas. Mora no domínio porque passa da porta de saída até a web.
 */
public record UnitSummary(
        EstablishmentId id,
        String name,
        Slug slug,
        Instant createdAt,
        long activeTables,
        long activeTeam
) {
    public UnitSummary {
        Objects.requireNonNull(id);
        Objects.requireNonNull(slug);
        Objects.requireNonNull(createdAt);
    }
}
