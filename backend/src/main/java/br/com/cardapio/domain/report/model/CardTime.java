package br.com.cardapio.domain.report.model;

import java.time.Duration;
import java.util.Objects;

/** Quanto tempo os cartões da passagem ficaram verdes e vermelhos (REL-74). */
public record CardTime(Duration serving, Duration paused) {

    public CardTime {
        Objects.requireNonNull(serving, "tempo verde é obrigatório");
        Objects.requireNonNull(paused, "tempo vermelho é obrigatório");
    }
}
