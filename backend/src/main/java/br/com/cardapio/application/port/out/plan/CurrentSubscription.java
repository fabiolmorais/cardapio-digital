package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.Subscription;

/** A assinatura do restaurante com o plano dela. */
public record CurrentSubscription(Subscription subscription, Plan plan) {
}
