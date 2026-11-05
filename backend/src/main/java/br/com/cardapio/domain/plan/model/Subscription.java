package br.com.cardapio.domain.plan.model;

import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.plan.exception.PlanNotSelfServiceException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A assinatura da empresa (organization): qual plano, e até quando está paga. Fica na empresa,
 * e não no restaurante, porque o Enterprise com várias unidades paga uma assinatura só.
 *
 * <p>A situação de agora ({@link #standing(Instant)}) é calculada com a data, e não gravada: o
 * teste acaba sozinho, sem um processo agendado para virar o status.
 */
public record Subscription(
        SubscriptionId id,
        OrganizationId organizationId,
        PlanId planId,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        Instant trialEndsAt,
        Instant currentPeriodEnd,
        Instant createdAt,
        PlanRequest request) {

    /** Teste grátis de 14 dias (especificação, "Nossos planos"). */
    public static final Duration TRIAL = Duration.ofDays(14);

    /** Depois de vencer, 7 dias com tudo funcionando (decisão de 05/11/2026). */
    public static final Duration GRACE = Duration.ofDays(7);

    /** O plano do teste grátis: o Pro, para o dono experimentar os vídeos (decisão de 05/11/2026). */
    public static final String TRIAL_PLAN_CODE = "pro";

    public Subscription {
        Objects.requireNonNull(id);
        Objects.requireNonNull(organizationId);
        Objects.requireNonNull(planId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(trialEndsAt);
        Objects.requireNonNull(createdAt);
        if ((status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.PAST_DUE)
                && currentPeriodEnd == null) {
            throw new IllegalArgumentException("assinatura paga precisa do fim do período");
        }
    }

    /** Sem plano pedido (o mesmo construtor do Guia 30). */
    public Subscription(SubscriptionId id, OrganizationId organizationId, PlanId planId, SubscriptionStatus status,
                        BillingCycle billingCycle, Instant trialEndsAt, Instant currentPeriodEnd, Instant createdAt) {
        this(id, organizationId, planId, status, billingCycle, trialEndsAt, currentPeriodEnd, createdAt, null);
    }

    /** O teste grátis que todo cadastro ganha, a partir de agora. */
    public static Subscription startTrial(OrganizationId organizationId, PlanId trialPlan, Instant now) {
        return new Subscription(SubscriptionId.newId(), organizationId, trialPlan, SubscriptionStatus.TRIALING,
                null, now.plus(TRIAL), null, now);
    }

    /** Até quando está coberta: o fim do teste ou o fim do período pago. */
    public Instant coveredUntil() {
        return status == SubscriptionStatus.TRIALING ? trialEndsAt : currentPeriodEnd;
    }

    /** Até quando a tolerância vai, depois de vencer. */
    public Instant graceEndsAt() {
        return coveredUntil().plus(GRACE);
    }

    /**
     * O dono escolhe o plano e o ciclo. A escolha substitui a anterior e só vale depois do
     * pagamento: até lá, os limites continuam os do plano atual. O Enterprise é sob consulta.
     */
    public Subscription requestPlan(Plan plan, BillingCycle cycle, Instant now) {
        if (plan.isCustomPricing()) {
            throw new PlanNotSelfServiceException(plan.name());
        }
        return new Subscription(id, organizationId, planId, status, billingCycle, trialEndsAt, currentPeriodEnd,
                createdAt, new PlanRequest(plan.id(), cycle, now));
    }

    public Standing standing(Instant now) {
        if (status == SubscriptionStatus.CANCELED) {
            return Standing.SUSPENDED;
        }
        if (now.isBefore(coveredUntil())) {
            return status == SubscriptionStatus.TRIALING ? Standing.TRIAL : Standing.ACTIVE;
        }
        return now.isBefore(graceEndsAt()) ? Standing.PENDING : Standing.SUSPENDED;
    }
}
