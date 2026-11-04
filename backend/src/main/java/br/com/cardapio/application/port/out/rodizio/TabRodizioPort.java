package br.com.cardapio.application.port.out.rodizio;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.model.TabRodizio;

import java.util.Optional;

public interface TabRodizioPort {

    /**
     * Grava o rodízio da comanda, a menos que ela já tenha um (dois garçons ativando juntos).
     * false: já havia um, e nada foi gravado.
     */
    boolean insertIfNone(TabRodizio tabRodizio);

    Optional<TabRodizio> findByTab(EstablishmentId establishmentId, TabId tabId);

    /** Grava as pessoas novas (o resto do rodízio da comanda não muda). */
    void updateGuests(TabRodizio tabRodizio);

    /** Grava a cor do cartão e a hora da virada. */
    void updateStatus(TabRodizio tabRodizio);

    /** As mesas com a comanda aberta no rodízio de passagem: as pausadas primeiro, depois pela mesa. */
    ResultPage<PassagemTableView> findOpenPassagem(EstablishmentId establishmentId, PageQuery page);
}
