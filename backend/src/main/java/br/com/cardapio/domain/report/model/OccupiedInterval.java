package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.floor.model.DiningTableId;

import java.time.Instant;
import java.util.Objects;

/** Uma mesa com comanda aberta de {@code from} até {@code to} (exclusivo). */
public record OccupiedInterval(DiningTableId table, Instant from, Instant to) {

    public OccupiedInterval {
        Objects.requireNonNull(table, "mesa é obrigatória");
        Objects.requireNonNull(from, "início é obrigatório");
        Objects.requireNonNull(to, "fim é obrigatório");
    }
}
