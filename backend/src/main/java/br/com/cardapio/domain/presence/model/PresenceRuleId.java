package br.com.cardapio.domain.presence.model;

import java.util.Objects;
import java.util.UUID;

public record PresenceRuleId(UUID value) {

    public PresenceRuleId {
        Objects.requireNonNull(value, "id da regra de presença é obrigatório");
    }

    public static PresenceRuleId newId() {
        return new PresenceRuleId(UUID.randomUUID());
    }
}
