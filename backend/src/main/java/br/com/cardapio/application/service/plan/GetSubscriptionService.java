package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.in.plan.GetSubscriptionUseCase;
import br.com.cardapio.application.port.in.plan.SubscriptionOverview;
import br.com.cardapio.application.port.out.plan.CurrentSubscription;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class GetSubscriptionService implements GetSubscriptionUseCase {

    private final SubscriptionPort subscriptions;
    private final Clock clock;

    public GetSubscriptionService(SubscriptionPort subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionOverview get(EstablishmentId establishmentId) {
        CurrentSubscription current = PlanAllowances.require(subscriptions.find(establishmentId));
        return SubscriptionOverviews.of(current, subscriptions.usage(establishmentId), clock.instant());
    }
}
