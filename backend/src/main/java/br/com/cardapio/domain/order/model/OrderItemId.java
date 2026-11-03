package br.com.cardapio.domain.order.model;

import java.util.Objects;
import java.util.UUID;

public record OrderItemId(UUID value) {

    public OrderItemId {
        Objects.requireNonNull(value, "id do item do pedido é obrigatório");
    }

    public static OrderItemId newId() {
        return new OrderItemId(UUID.randomUUID());
    }
}
