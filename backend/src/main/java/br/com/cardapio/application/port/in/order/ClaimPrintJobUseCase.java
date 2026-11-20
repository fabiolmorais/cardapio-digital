package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.TicketKind;
import br.com.cardapio.domain.order.model.TabMoveId;

public interface ClaimPrintJobUseCase {

    /**
     * A estação de impressão pega a comanda deste pedido: os itens que faltavam saem marcados como
     * impressos, e só ela os recebe (outra estação aberta recebe a lista vazia e não imprime de novo).
     * Na comanda do pedido, os itens que estavam na fila passam para "em preparo".
     */
    PrintJob claim(EstablishmentId establishmentId, OrderId orderId, TicketKind kind);

    /**
     * A estação pega a via "MUDOU DE MESA": ela sai marcada como impressa, e só esta estação a recebe
     * (outra recebe a via sem itens e não imprime). Sem itens também se tudo já chegou à mesa.
     */
    MovePrintJob claimMove(EstablishmentId establishmentId, TabMoveId moveId);
}
