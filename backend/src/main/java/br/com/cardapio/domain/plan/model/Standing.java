package br.com.cardapio.domain.plan.model;

/** A situação da assinatura agora, do ponto de vista do restaurante. */
public enum Standing {
    /** No teste grátis: tudo funciona. */
    TRIAL,
    /** Paga: tudo funciona. */
    ACTIVE,
    /** Venceu (o teste ou o período pago), dentro da tolerância: tudo funciona, com o aviso no admin. */
    PENDING,
    /** Passou da tolerância: a mesa para de chamar e pedir; o admin continua abrindo. */
    SUSPENDED;

    /** A mesa ainda chama o garçom e faz pedidos. */
    public boolean serving() {
        return this != SUSPENDED;
    }
}
