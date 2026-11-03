package br.com.cardapio.domain.order.model;

/** O pedido inteiro (o "envio"). Cada item tem o seu próprio status (OrderItemStatus). */
public enum OrderStatus {
    /** Parado na confirmação do garçom (a confirmação está ligada, ou o pedido passou de um limite). */
    PENDING_APPROVAL,
    /** Foi para a cozinha. */
    SENT,
    /** O garçom recusou: os itens ficam cancelados e nada entra na conta. */
    REJECTED
}
