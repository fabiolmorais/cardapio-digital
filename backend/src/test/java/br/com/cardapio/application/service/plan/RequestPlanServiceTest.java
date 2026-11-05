package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.in.plan.RequestPlanCommand;
import br.com.cardapio.application.port.in.plan.SubscriptionOverview;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.plan.exception.PlanNotFoundException;
import br.com.cardapio.domain.plan.exception.PlanNotSelfServiceException;
import br.com.cardapio.domain.plan.model.BillingCycle;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.support.InMemorySubscriptions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestPlanServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions()
            .given(InMemorySubscriptions.PRO, new PlanUsage(12, 4, 2));
    private final RequestPlanService service = new RequestPlanService(subscriptions, subscriptions,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void theOwnerChoosesThePlanAndTheCycle() {
        SubscriptionOverview overview = service.request(new RequestPlanCommand(sakura, "essential", BillingCycle.ANNUAL));

        assertThat(overview.request().plan().name()).isEqualTo("Essencial");
        assertThat(overview.request().cycle()).isEqualTo(BillingCycle.ANNUAL);
        assertThat(overview.request().requestedAt()).isEqualTo(NOW);
        assertThat(overview.plan().name()).isEqualTo("Pro"); // vale depois do pagamento
        assertThat(subscriptions.locks).isEqualTo(1);

        // Mudou de ideia: a escolha nova substitui a anterior.
        service.request(new RequestPlanCommand(sakura, "pro", BillingCycle.MONTHLY));
        assertThat(subscriptions.saved).hasSize(2);
        assertThat(subscriptions.saved.getLast().request().cycle()).isEqualTo(BillingCycle.MONTHLY);
    }

    @Test
    void unknownPlansAndTheEnterpriseAreRefused() {
        assertThatThrownBy(() -> service.request(new RequestPlanCommand(sakura, "ouro", BillingCycle.MONTHLY)))
                .isInstanceOf(PlanNotFoundException.class);
        assertThatThrownBy(() -> service.request(new RequestPlanCommand(sakura, "enterprise", BillingCycle.MONTHLY)))
                .isInstanceOf(PlanNotSelfServiceException.class);
        assertThat(subscriptions.saved).isEmpty();
    }
}
