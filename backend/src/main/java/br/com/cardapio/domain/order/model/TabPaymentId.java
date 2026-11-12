package br.com.cardapio.domain.order.model;

import java.util.Objects;
import java.util.UUID;

public record TabPaymentId(UUID value) {

    public TabPaymentId {
        Objects.requireNonNull(value, "id do pagamento é obrigatório");
    }

    public static TabPaymentId newId() {
        return new TabPaymentId(UUID.randomUUID());
    }
}
