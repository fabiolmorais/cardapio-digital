package br.com.cardapio.domain.order.model;

import java.util.Objects;
import java.util.UUID;

public record OrderSettingsId(UUID value) {

    public OrderSettingsId {
        Objects.requireNonNull(value, "id das configurações de pedido é obrigatório");
    }

    public static OrderSettingsId newId() {
        return new OrderSettingsId(UUID.randomUUID());
    }
}
