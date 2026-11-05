package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.Subscription;

/**
 * A assinatura do restaurante com o plano dela.
 *
 * @param requestedPlan o plano que o dono pediu, à espera do pagamento (null se não pediu)
 */
public record CurrentSubscription(Subscription subscription, Plan plan, Plan requestedPlan) {

    /** Sem plano pedido. */
    public CurrentSubscription(Subscription subscription, Plan plan) {
        this(subscription, plan, null);
    }
}
