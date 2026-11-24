package br.com.cardapio.domain.report.model;

import java.util.Objects;

/** As pessoas de uma faixa do rodízio ("Adulto", "Criança") no período. */
public record TierCount(String tier, int people) {

    public TierCount {
        Objects.requireNonNull(tier, "faixa é obrigatória");
    }
}
