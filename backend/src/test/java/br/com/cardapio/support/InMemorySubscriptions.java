package br.com.cardapio.support;

import br.com.cardapio.application.port.out.plan.CurrentSubscription;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.plan.model.PlanId;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.plan.model.Subscription;
import br.com.cardapio.domain.plan.model.SubscriptionStatus;
import br.com.cardapio.domain.plan.model.SupportLevel;
import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A assinatura dos testes sem banco: um plano só para todo restaurante e o uso que o teste
 * mandar. Começa num plano sem limites (os testes de cadastro não pensam no plano) e grava o que
 * o cadastro salvar. Também serve de catálogo de planos (o Essencial e o Pro, como na V1).
 */
public class InMemorySubscriptions implements SubscriptionPort, LoadPlansPort {

    public static final Plan ESSENTIAL = plan("essential", "Essencial", Limit.of(20), Limit.of(5), Limit.of(1), Set.of());
    public static final Plan PRO = plan("pro", "Pro", Limit.of(50), Limit.unlimited(), Limit.of(3),
            Set.of(PlanFeature.PRODUCT_VIDEOS));
    public static final Plan UNLIMITED = plan("test", "Sem limites", Limit.unlimited(), Limit.unlimited(),
            Limit.unlimited(), Set.of(PlanFeature.PRODUCT_VIDEOS));
    /** Sob consulta: sem preço. */
    public static final Plan ENTERPRISE = new Plan(new PlanId(UUID.nameUUIDFromBytes("enterprise".getBytes())),
            "enterprise", "Enterprise", null, null, Limit.unlimited(), Limit.unlimited(), Limit.unlimited(), 0,
            Set.of(PlanFeature.PRODUCT_VIDEOS), SupportLevel.DEDICATED_MANAGER, 4);
    private static final List<Plan> CATALOG = List.of(ESSENTIAL, PRO, ENTERPRISE);

    public final List<Subscription> saved = new ArrayList<>();
    private Plan plan = UNLIMITED;
    private PlanUsage usage = new PlanUsage(0, 0, 0);
    public int locks;
    /** A última assinatura gravada (o pedido de plano) ou a suspensa; null: o teste grátis padrão. */
    private Subscription stored;

    /** Daqui em diante, todo restaurante está neste plano e usa isto. */
    public InMemorySubscriptions given(Plan plan, PlanUsage usage) {
        this.plan = plan;
        this.usage = usage;
        return this;
    }

    private static Plan plan(String code, String name, Limit tables, Limit users, Limit sectors,
                             Set<PlanFeature> features) {
        return new Plan(new PlanId(UUID.nameUUIDFromBytes(code.getBytes())), code, name, Money.ofCents(6990),
                Money.ofCents(69900), tables, users, sectors, 90, features, SupportLevel.EMAIL, 1);
    }

    /** A assinatura cancelada: a mesa para de chamar e pedir. */
    public InMemorySubscriptions suspended() {
        Subscription trial = Subscription.startTrial(OrganizationId.newId(), plan.id(), Instant.parse("2026-11-05T12:00:00Z"));
        stored = new Subscription(trial.id(), trial.organizationId(), trial.planId(), SubscriptionStatus.CANCELED, null,
                trial.trialEndsAt(), null, trial.createdAt());
        return this;
    }

    private CurrentSubscription current() {
        if (stored == null) {
            stored = Subscription.startTrial(OrganizationId.newId(), plan.id(), Instant.parse("2026-11-05T12:00:00Z"));
        }
        Plan requested = stored.request() == null ? null : CATALOG.stream()
                .filter(p -> p.id().equals(stored.request().planId())).findFirst().orElseThrow();
        return new CurrentSubscription(stored, plan, requested);
    }

    @Override
    public void save(Subscription subscription) {
        saved.add(subscription);
        stored = subscription;
    }

    @Override
    public Optional<CurrentSubscription> find(EstablishmentId establishmentId) {
        return Optional.of(current());
    }

    @Override
    public Optional<CurrentSubscription> lock(EstablishmentId establishmentId) {
        locks++;
        return Optional.of(current());
    }

    @Override
    public PlanUsage usage(EstablishmentId establishmentId) {
        return usage;
    }

    @Override
    public ResultPage<Plan> findActive(PageQuery query) {
        return new ResultPage<>(CATALOG, query.page(), query.size(), CATALOG.size());
    }

    @Override
    public Optional<Plan> findActiveByCode(String code) {
        return CATALOG.stream().filter(p -> p.code().equals(code)).findFirst();
    }
}
