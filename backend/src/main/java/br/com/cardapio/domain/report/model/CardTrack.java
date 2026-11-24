package br.com.cardapio.domain.report.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** O cartão de uma mesa de passagem: começa verde no início do rodízio e vai até o fechamento. */
public record CardTrack(Instant start, Instant end, List<CardChange> changes) {

    public CardTrack {
        Objects.requireNonNull(start, "início é obrigatório");
        Objects.requireNonNull(end, "fim é obrigatório");
        changes = List.copyOf(changes);
    }
}
