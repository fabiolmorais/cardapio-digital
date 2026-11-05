package br.com.cardapio.adapter.out.persistence.plan;

import br.com.cardapio.application.port.out.plan.CurrentSubscription;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.plan.model.BillingCycle;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanId;
import br.com.cardapio.domain.plan.model.PlanRequest;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.plan.model.Subscription;
import br.com.cardapio.domain.plan.model.SubscriptionId;
import br.com.cardapio.domain.plan.model.SubscriptionStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class SubscriptionPersistenceAdapter implements SubscriptionPort {

    private final SubscriptionJpaRepository subscriptions;
    private final PlanJpaRepository plans;

    SubscriptionPersistenceAdapter(SubscriptionJpaRepository subscriptions, PlanJpaRepository plans) {
        this.subscriptions = subscriptions;
        this.plans = plans;
    }

    @Override
    public void save(Subscription s) {
        PlanRequest r = s.request();
        subscriptions.save(new SubscriptionJpaEntity(s.id().value(), s.organizationId().value(), s.planId().value(),
                s.status().name(), s.billingCycle() == null ? null : s.billingCycle().name(), s.trialEndsAt(),
                s.currentPeriodEnd(), s.createdAt(), r == null ? null : r.planId().value(),
                r == null ? null : r.cycle().name(), r == null ? null : r.requestedAt()));
    }

    @Override
    public Optional<CurrentSubscription> find(EstablishmentId establishmentId) {
        return subscriptions.findByEstablishment(establishmentId.value()).map(this::withPlan);
    }

    @Override
    public Optional<CurrentSubscription> lock(EstablishmentId establishmentId) {
        return subscriptions.lockByEstablishment(establishmentId.value()).map(this::withPlan);
    }

    @Override
    public PlanUsage usage(EstablishmentId establishmentId) {
        var row = subscriptions.usage(establishmentId.value());
        return new PlanUsage(row.getTables(), row.getUsers(), row.getSectors());
    }

    private CurrentSubscription withPlan(SubscriptionJpaEntity e) {
        Subscription subscription = toDomain(e);
        // O plano pode ter saído da vitrine (active = false) e continuar valendo para quem já assina.
        Plan plan = plan(e.getPlanId());
        Plan requested = e.getRequestedPlanId() == null ? null : plan(e.getRequestedPlanId());
        return new CurrentSubscription(subscription, plan, requested);
    }

    private Plan plan(UUID id) {
        return plans.findById(id)
                .map(PlanPersistenceMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("plano da assinatura não encontrado"));
    }

    static Subscription toDomain(SubscriptionJpaEntity e) {
        PlanRequest request = e.getRequestedPlanId() == null ? null
                : new PlanRequest(new PlanId(e.getRequestedPlanId()), BillingCycle.valueOf(e.getRequestedCycle()),
                e.getRequestedAt());
        return new Subscription(new SubscriptionId(e.getId()), new OrganizationId(e.getOrganizationId()),
                new PlanId(e.getPlanId()), SubscriptionStatus.valueOf(e.getStatus()),
                e.getBillingCycle() == null ? null : BillingCycle.valueOf(e.getBillingCycle()),
                e.getTrialEndsAt(), e.getCurrentPeriodEnd(), e.getCreatedAt(), request);
    }
}
