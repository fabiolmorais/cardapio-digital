package br.com.cardapio.domain.order.model;

/** O caminho de um item: na fila → em preparo → pronto → entregue, ou cancelado (especificação). */
public enum OrderItemStatus {
    /** Na fila da cozinha (ou esperando a confirmação do garçom, se o pedido estiver parado nela). */
    PENDING,
    PREPARING,
    READY,
    DELIVERED,
    CANCELED;

    /** Ainda não chegou à mesa nem foi cancelado: é o que aparece em "A caminho". */
    public boolean isOnTheWay() {
        return this == PENDING || this == PREPARING || this == READY;
    }
}
