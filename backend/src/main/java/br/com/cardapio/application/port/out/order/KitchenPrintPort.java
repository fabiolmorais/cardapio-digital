package br.com.cardapio.application.port.out.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.TicketKind;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * O que a impressora da cozinha ainda precisa imprimir, e o registro do que já saiu. Vale o mesmo
 * que a fila da cozinha: só pedidos que foram para a cozinha, e sem os cortes do rodízio de passagem.
 */
public interface KitchenPrintPort {

    /**
     * As comandas pendentes deste tipo, da mais antiga à mais nova: ORDER, os itens a caminho
     * ainda sem comanda; CANCELLATION, os cancelados depois de impressos, ainda sem a via "CANCELADO".
     */
    ResultPage<PrintJobView> findPending(EstablishmentId establishmentId, TicketKind kind, PageQuery page);

    /** As comandas que saíram na impressora desde aquele instante, da mais recente para a mais antiga. */
    ResultPage<PrintJobView> findPrintedSince(EstablishmentId establishmentId, Instant since, PageQuery page);

    /**
     * Trava (FOR UPDATE) e devolve os itens deste pedido que ainda faltam nesta via. Duas estações
     * pedindo a mesma comanda: a segunda espera a primeira e encontra a lista vazia.
     * Vazio (Optional) se o pedido não é deste restaurante.
     */
    Optional<PrintJobView> lockPending(EstablishmentId establishmentId, OrderId orderId, TicketKind kind);

    /** Marca estes itens como impressos nesta via. */
    void markPrinted(EstablishmentId establishmentId, Collection<OrderItemId> itemIds, TicketKind kind, Instant now);
}
