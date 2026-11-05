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

    public final List<Subscription> saved = new ArrayList<>();
    private Plan plan = UNLIMITED;
    private PlanUsage usage = new PlanUsage(0, 0, 0);
    public int locks;

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

    private CurrentSubscription current() {
        Instant now = Instant.parse("2026-11-05T12:00:00Z");
        return new CurrentSubscription(Subscription.startTrial(OrganizationId.newId(), plan.id(), now), plan);
    }

    @Override
    public void save(Subscription subscription) {
        saved.add(subscription);
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
        return new ResultPage<>(List.of(ESSENTIAL, PRO), query.page(), query.size(), 2);
    }

    @Override
    public Optional<Plan> findActiveByCode(String code) {
        return List.of(ESSENTIAL, PRO).stream().filter(p -> p.code().equals(code)).findFirst();
    }
}
