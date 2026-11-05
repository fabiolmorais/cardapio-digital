package br.com.cardapio.domain.plan.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Os avisos por e-mail do fim do teste grátis. Cada um sai uma vez por assinatura; se a tarefa
 * passar de um marco sem enviar (servidor parado), sai só o mais recente, e não os atrasados.
 */
public enum TrialNotice {
    /** 3 dias antes do fim. */
    ENDS_IN_3_DAYS,
    /** 1 dia antes do fim. */
    ENDS_TOMORROW,
    /** Acabou: começa a tolerância. */
    ENDED;

    /** O aviso da vez para esta assinatura agora, se houver. */
    public static Optional<TrialNotice> due(Subscription subscription, Instant now) {
        if (subscription.status() != SubscriptionStatus.TRIALING) {
            return Optional.empty();
        }
        Instant end = subscription.trialEndsAt();
        if (!now.isBefore(subscription.graceEndsAt())) {
            return Optional.empty(); // já suspensa: o aviso do fim saiu antes
        }
        if (!now.isBefore(end)) {
            return Optional.of(ENDED);
        }
        if (!now.isBefore(end.minus(Duration.ofDays(1)))) {
            return Optional.of(ENDS_TOMORROW);
        }
        if (!now.isBefore(end.minus(Duration.ofDays(3)))) {
            return Optional.of(ENDS_IN_3_DAYS);
        }
        return Optional.empty();
    }
}
