package br.com.cardapio.application.port.in.order;

/** Os passos que a equipe dá num item: a cozinha começa e marca pronto; o garçom entrega. */
public enum OrderItemStep {
    START,
    READY,
    DELIVER
}
