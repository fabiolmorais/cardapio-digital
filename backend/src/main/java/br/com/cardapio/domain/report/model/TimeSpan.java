package br.com.cardapio.domain.report.model;

import java.time.Instant;
import java.util.Objects;

/** Um intervalo [início, fim) no tempo. */
public record TimeSpan(Instant start, Instant end) {

    public TimeSpan {
        Objects.requireNonNull(start, "início é obrigatório");
        Objects.requireNonNull(end, "fim é obrigatório");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("o intervalo precisa começar antes de terminar");
        }
    }
}
