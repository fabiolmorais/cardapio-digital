package br.com.cardapio.domain.order.model;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "id do pedido é obrigatório");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
