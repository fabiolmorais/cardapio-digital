package br.com.cardapio.application.port.in.plan;

import br.com.cardapio.domain.plan.model.BillingCycle;
import br.com.cardapio.domain.plan.model.Plan;

import java.time.Instant;

/** O plano que o dono pediu na tela, à espera do pagamento. */
public record RequestedPlan(Plan plan, BillingCycle cycle, Instant requestedAt) {
}
