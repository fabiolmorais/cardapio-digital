package br.com.cardapio.domain.order.model;

/** A via impressa para a cozinha: a comanda do pedido, ou o aviso de que itens dela foram cancelados. */
public enum TicketKind {
    /** Os itens novos do pedido, para preparar. */
    ORDER,
    /** Os itens que já tinham saído na impressora e foram cancelados depois. */
    CANCELLATION
}
