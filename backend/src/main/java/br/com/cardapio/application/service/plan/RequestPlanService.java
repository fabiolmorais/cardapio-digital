package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.in.plan.RequestPlanCommand;
import br.com.cardapio.application.port.in.plan.RequestPlanUseCase;
import br.com.cardapio.application.port.in.plan.SubscriptionOverview;
import br.com.cardapio.application.port.out.plan.CurrentSubscription;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.domain.plan.exception.PlanNotFoundException;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.Subscription;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class RequestPlanService implements RequestPlanUseCase {

    private final SubscriptionPort subscriptions;
    private final LoadPlansPort plans;
    private final Clock clock;

    public RequestPlanService(SubscriptionPort subscriptions, LoadPlansPort plans, Clock clock) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionOverview request(RequestPlanCommand command) {
        Instant now = clock.instant();
        Plan plan = plans.findActiveByCode(command.planCode()).orElseThrow(PlanNotFoundException::new);
        CurrentSubscription current = PlanAllowances.require(subscriptions.lock(command.establishmentId()));
        Subscription requested = current.subscription().requestPlan(plan, command.cycle(), now);
        subscriptions.save(requested);
        return SubscriptionOverviews.of(new CurrentSubscription(requested, current.plan(), plan),
                subscriptions.usage(command.establishmentId()), now);
    }
}
