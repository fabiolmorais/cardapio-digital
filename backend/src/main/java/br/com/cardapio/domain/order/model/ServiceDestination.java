package br.com.cardapio.domain.order.model;

/** Para quem fica registrada a taxa de serviço. O sistema registra; o repasse é do restaurante. */
public enum ServiceDestination {
    /** Caixinha da equipe: um total só, que o restaurante divide. */
    TEAM,
    /** O garçom que mais atendeu a mesa (chamados aceitos e itens entregues). */
    WAITER
}
