package br.com.cardapio.domain.order.model;

import java.util.Objects;
import java.util.UUID;

public record TabId(UUID value) {

    public TabId {
        Objects.requireNonNull(value, "id da comanda é obrigatório");
    }

    public static TabId newId() {
        return new TabId(UUID.randomUUID());
    }
}
