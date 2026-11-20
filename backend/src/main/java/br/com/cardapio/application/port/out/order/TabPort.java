package br.com.cardapio.application.port.out.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeriod;

import java.util.Optional;

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

    /** A comanda com a mesa e o setor, aberta ou fechada. */
    Optional<TabView> findView(EstablishmentId establishmentId, TabId tabId);

    /** Grava o fechamento SÓ SE a comanda ainda estiver aberta (o UPDATE condicional). */
    boolean close(Tab closed);

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
