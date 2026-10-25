package br.com.cardapio.domain.plan.model;

import java.util.Objects;
import java.util.UUID;

public record PlanId(UUID value) {

    public PlanId {
        Objects.requireNonNull(value, "id do plano é obrigatório");
    }
}
