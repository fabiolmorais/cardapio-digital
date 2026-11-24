package br.com.cardapio.domain.report.model;

import java.util.Objects;

/** Um produto pedido no rodízio e quantas unidades (REL-72). */
public record RodizioItem(String name, int quantity) {

    public RodizioItem {
        Objects.requireNonNull(name, "nome é obrigatório");
    }
}
