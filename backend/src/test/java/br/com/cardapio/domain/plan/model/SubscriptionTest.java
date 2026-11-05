package br.com.cardapio.domain.plan.model;

import br.com.cardapio.domain.account.model.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private static final Instant SIGNUP = Instant.parse("2026-11-05T12:00:00Z");
    private final PlanId pro = new PlanId(UUID.randomUUID());
    private final Subscription trial = Subscription.startTrial(OrganizationId.newId(), pro, SIGNUP);

    private static Instant days(double days) {
        return SIGNUP.plus(Duration.ofMinutes(Math.round(days * 24 * 60)));
    }

    @Test
    void theTrialLastsFourteenDays() {
        assertThat(trial.status()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(trial.coveredUntil()).isEqualTo(Instant.parse("2026-11-19T12:00:00Z"));
        assertThat(trial.graceEndsAt()).isEqualTo(Instant.parse("2026-11-26T12:00:00Z"));
    }

    @Test
    void afterTheTrialSevenDaysOfGraceThenTheTableStops() {
        assertThat(trial.standing(days(13.9))).isEqualTo(Standing.TRIAL);
        assertThat(trial.standing(days(14))).isEqualTo(Standing.PENDING);
        assertThat(trial.standing(days(20.9))).isEqualTo(Standing.PENDING);
        assertThat(trial.standing(days(21))).isEqualTo(Standing.SUSPENDED);

        assertThat(Standing.PENDING.serving()).isTrue();
        assertThat(Standing.SUSPENDED.serving()).isFalse();
    }

    @Test
    void aPaidSubscriptionFollowsItsPeriodAndCancelingStopsIt() {
        Instant periodEnd = days(45);
        Subscription paid = new Subscription(trial.id(), trial.organizationId(), pro, SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY, trial.trialEndsAt(), periodEnd, SIGNUP);
        Subscription canceled = new Subscription(trial.id(), trial.organizationId(), pro, SubscriptionStatus.CANCELED,
                null, trial.trialEndsAt(), null, SIGNUP);

        assertThat(paid.standing(days(30))).isEqualTo(Standing.ACTIVE); // o fim do teste não importa mais
        assertThat(paid.standing(days(46))).isEqualTo(Standing.PENDING);
        assertThat(paid.standing(days(52))).isEqualTo(Standing.SUSPENDED);
        assertThat(canceled.standing(days(1))).isEqualTo(Standing.SUSPENDED);
    }

    @Test
    void aPaidSubscriptionAlwaysKnowsItsPeriodEnd() {
        assertThatThrownBy(() -> new Subscription(trial.id(), trial.organizationId(), pro, SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY, trial.trialEndsAt(), null, SIGNUP))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
