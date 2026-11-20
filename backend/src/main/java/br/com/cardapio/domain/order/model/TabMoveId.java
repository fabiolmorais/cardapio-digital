package br.com.cardapio.domain.order.model;

import java.util.Objects;
import java.util.UUID;

public record TabMoveId(UUID value) {

    public TabMoveId {
        Objects.requireNonNull(value, "id da mudança de mesa é obrigatório");
    }

    public static TabMoveId newId() {
        return new TabMoveId(UUID.randomUUID());
    }
}
