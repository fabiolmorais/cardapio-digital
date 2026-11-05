package br.com.cardapio.domain.plan.model;

import java.time.Instant;
import java.util.Objects;

/**
 * O plano que o dono escolheu na tela "Assinatura", à espera do pagamento. Até a cobrança
 * automática (fase 2), o pagamento é combinado por fora, e nós ativamos pelo script de ativação.
 */
public record PlanRequest(PlanId planId, BillingCycle cycle, Instant requestedAt) {

    public PlanRequest {
        Objects.requireNonNull(planId);
        Objects.requireNonNull(cycle);
        Objects.requireNonNull(requestedAt);
    }
}
