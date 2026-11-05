package br.com.cardapio.domain.plan.model;

import br.com.cardapio.domain.account.model.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrialNoticeTest {

    private static final Instant END = Instant.parse("2026-11-19T12:00:00Z");
    private final Subscription trial = Subscription.startTrial(OrganizationId.newId(), new PlanId(UUID.randomUUID()),
            END.minus(Subscription.TRIAL));

    private Optional<TrialNotice> dueAt(Duration beforeEnd) {
        return TrialNotice.due(trial, END.minus(beforeEnd));
    }

    @Test
    void threeDaysBeforeOneDayBeforeAndAtTheEnd() {
        assertThat(dueAt(Duration.ofDays(4))).isEmpty();
        assertThat(dueAt(Duration.ofDays(3))).contains(TrialNotice.ENDS_IN_3_DAYS);
        assertThat(dueAt(Duration.ofHours(25))).contains(TrialNotice.ENDS_IN_3_DAYS);
        assertThat(dueAt(Duration.ofDays(1))).contains(TrialNotice.ENDS_TOMORROW);
        assertThat(dueAt(Duration.ZERO)).contains(TrialNotice.ENDED);
        assertThat(dueAt(Duration.ofDays(-6))).contains(TrialNotice.ENDED);
    }

    @Test
    void nothingAfterTheGraceOrForAPaidSubscription() {
        assertThat(dueAt(Duration.ofDays(-7))).isEmpty();
        Subscription paid = new Subscription(trial.id(), trial.organizationId(), trial.planId(),
                SubscriptionStatus.ACTIVE, BillingCycle.MONTHLY, END, END.plus(Duration.ofDays(30)), trial.createdAt());
        assertThat(TrialNotice.due(paid, END.minus(Duration.ofDays(1)))).isEmpty();
    }
}
