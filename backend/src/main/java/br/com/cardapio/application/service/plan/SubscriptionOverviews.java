package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.in.plan.RequestedPlan;
import br.com.cardapio.application.port.in.plan.SubscriptionOverview;
import br.com.cardapio.application.port.out.plan.CurrentSubscription;
import br.com.cardapio.domain.plan.model.PlanRequest;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.plan.model.Subscription;

import java.time.Instant;

/** Monta a visão do dono a partir da assinatura, do uso e da hora (a consulta e o pedido de plano usam). */
final class SubscriptionOverviews {

    private SubscriptionOverviews() {
    }

    static SubscriptionOverview of(CurrentSubscription current, PlanUsage usage, Instant now) {
        Subscription subscription = current.subscription();
        PlanRequest request = subscription.request();
        RequestedPlan requested = request == null || current.requestedPlan() == null ? null
                : new RequestedPlan(current.requestedPlan(), request.cycle(), request.requestedAt());
        return new SubscriptionOverview(current.plan(), subscription.standing(now), subscription.coveredUntil(),
                subscription.graceEndsAt(), usage, requested);
    }
}
