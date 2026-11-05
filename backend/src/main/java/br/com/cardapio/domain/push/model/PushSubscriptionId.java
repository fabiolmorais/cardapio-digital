package br.com.cardapio.domain.push.model;

import java.util.Objects;
import java.util.UUID;

public record PushSubscriptionId(UUID value) {

    public PushSubscriptionId {
        Objects.requireNonNull(value);
    }

    public static PushSubscriptionId newId() {
        return new PushSubscriptionId(UUID.randomUUID());
    }
}
