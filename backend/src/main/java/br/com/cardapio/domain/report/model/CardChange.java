package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;

import java.time.Instant;
import java.util.Objects;

/** Uma virada do cartão da passagem: verde (SERVING) ou vermelho (PAUSED). */
public record CardChange(TabRodizioStatus status, Instant at) {

    public CardChange {
        Objects.requireNonNull(status, "cor é obrigatória");
        Objects.requireNonNull(at, "hora é obrigatória");
    }
}
