package br.com.cardapio.application.port.out.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeriod;

import java.util.Optional;
import br.com.cardapio.domain.order.model.ServiceDestination;
import br.com.cardapio.domain.account.model.UserId;

public interface TabPort {

    /** A comanda aberta da mesa, se houver (no máximo uma: o banco garante). */
    Optional<Tab> findOpen(EstablishmentId establishmentId, DiningTableId tableId);

    /**
     * Trava a comanda aberta até o fim da transação: dois pedidos da mesma mesa passam um de cada
     * vez, e o segundo enxerga o primeiro. false: ela já foi fechada.
     */
    boolean lockOpen(EstablishmentId establishmentId, TabId tabId);

    /**
     * Abre a comanda, a menos que a mesa já tenha uma aberta (dois celulares mandando o primeiro
     * pedido no mesmo instante). false: já havia uma, e nada foi gravado.
     */
    boolean insertIfNoneOpen(Tab tab);

    /**
     * Abre a comanda do balcão (ADR 0015), já com o nome e o cartão. false: o cartão já está numa
     * comanda aberta do restaurante, e nada foi gravado.
     */
    boolean insertAtCounter(Tab tab);

    /** Grava o nome e o cartão da comanda aberta. false: o cartão já está em outra comanda aberta. */
    boolean saveGuest(Tab tab);

    /** As comandas abertas de um balcão, com o total e os itens a caminho, da mais antiga para a mais nova. */
    ResultPage<OpenTabView> findOpenAtCounter(EstablishmentId establishmentId, DiningTableId counterId,
                                              PageQuery page);

    /** A comanda com a mesa e o setor, aberta ou fechada. */
    Optional<TabView> findView(EstablishmentId establishmentId, TabId tabId);

    /**
     * Grava o fechamento SÓ SE a comanda ainda estiver aberta (o UPDATE condicional), com a taxa de
     * serviço: para quem ficou e, se for do garçom, qual (null quando não houve taxa).
     */
    boolean close(Tab closed, ServiceDestination destination, UserId waiter);

    /** Grava a taxa escolhida para a comanda aberta. false: ela já foi fechada. */
    boolean saveService(Tab tab);

    /** Quem mais atendeu a mesa nesta comanda (chamados aceitos e itens entregues). */
    Optional<UserId> findTopAttendant(Tab tab);

    /** As comandas abertas com o total e os itens a caminho, numa consulta para a página. */
    ResultPage<OpenTabView> findOpenTabs(EstablishmentId establishmentId, PageQuery page);

    /**
     * As comandas fechadas nestes dias, no fuso do restaurante, da mais nova à mais antiga.
     *
     * @param tableId só desta mesa; null = todas
     */
    ResultPage<ClosedTabView> findClosed(EstablishmentId establishmentId, TabPeriod period, DiningTableId tableId,
                                         PageQuery page);

    /**
     * Grava a comanda depois da mudança de mesa (ADR 0012): a mesa e a abertura. Só se ela ainda
     * estiver aberta. false: fechou no meio do caminho, ou a mesa nova acabou de ganhar outra
     * comanda aberta (o índice "uma aberta por mesa").
     */
    boolean saveMoved(Tab tab);

    /** A comanda de origem se juntou a outra: MERGED, apontando para ela. Só se ainda estiver aberta. */
    boolean markMerged(EstablishmentId establishmentId, TabId tabId, TabId into);
}
