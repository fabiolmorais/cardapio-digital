package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.in.plan.SubscriptionOverview;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.plan.model.Standing;
import br.com.cardapio.support.InMemorySubscriptions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GetSubscriptionServiceTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions()
            .given(InMemorySubscriptions.PRO, new PlanUsage(12, 4, 2));

    private SubscriptionOverview at(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new GetSubscriptionService(subscriptions, clock).get(EstablishmentId.newId());
    }

    @Test
    void showsThePlanTheStandingTheDatesAndTheUsage() {
        // O falso começa o teste grátis em 05/11/2026 ao meio-dia.
        SubscriptionOverview overview = at("2026-11-10T12:00:00Z");

        assertThat(overview.plan().name()).isEqualTo("Pro");
        assertThat(overview.standing()).isEqualTo(Standing.TRIAL);
        assertThat(overview.coveredUntil()).isEqualTo(Instant.parse("2026-11-19T12:00:00Z"));
        assertThat(overview.graceEndsAt()).isEqualTo(Instant.parse("2026-11-26T12:00:00Z"));
        assertThat(overview.usage()).isEqualTo(new PlanUsage(12, 4, 2));
    }

    @Test
    void theStandingFollowsTheClock() {
        assertThat(at("2026-11-20T12:00:00Z").standing()).isEqualTo(Standing.PENDING);
        assertThat(at("2026-11-27T12:00:00Z").standing()).isEqualTo(Standing.SUSPENDED);
    }
}
