package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.tablesession.model.PresenceMethod;

import java.util.Objects;

/** Quantos celulares confirmaram a presença de um jeito (REL-66). */
public record PresenceCount(PresenceMethod method, int sessions) {

    public PresenceCount {
        Objects.requireNonNull(method, "forma de presença é obrigatória");
    }
}
