package br.com.cardapio.domain.plan.model;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionId(UUID value) {

    public SubscriptionId {
        Objects.requireNonNull(value);
    }

    public static SubscriptionId newId() {
        return new SubscriptionId(UUID.randomUUID());
    }
}
