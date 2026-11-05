package br.com.cardapio.domain.plan.model;

/**
 * O que está gravado na assinatura (como na especificação). A situação de agora, que depende
 * também da data, é o {@link Standing}.
 */
public enum SubscriptionStatus {
    /** No teste grátis. */
    TRIALING,
    /** Paga até {@code currentPeriodEnd}. */
    ACTIVE,
    /** A cobrança do período falhou (fase 2, com o gateway). */
    PAST_DUE,
    /** Encerrada. */
    CANCELED
}
