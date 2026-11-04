package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.ChangeRodizioGuestsUseCase;
import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

public class ChangeRodizioGuestsService implements ChangeRodizioGuestsUseCase {

    private final DiningTablePort tables;
    private final TabPort tabs;
    private final TabRodizioPort tabRodizios;
    private final EventPublisherPort events;
    private final Clock clock;

    public ChangeRodizioGuestsService(DiningTablePort tables, TabPort tabs, TabRodizioPort tabRodizios,
                                      EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.tabs = tabs;
        this.tabRodizios = tabRodizios;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Só na comanda aberta: depois da conta, o que foi cobrado não muda. A mesa de outro restaurante
     * é "não encontrada" (404), e não "sem rodízio": a resposta não conta nada da mesa dos outros.
     */
    @Override
    @Transactional
    public TableRodizio change(EstablishmentId establishmentId, DiningTableId tableId, Map<String, Integer> guests) {
        DiningTable table = tables.find(establishmentId, tableId).orElseThrow(TableNotFoundException::new);
        Tab tab = tabs.findOpen(establishmentId, table.id()).orElseThrow(NoRodizioOnTableException::new);
        TabRodizio current = tabRodizios.findByTab(establishmentId, tab.id())
                .orElseThrow(NoRodizioOnTableException::new);
        TabRodizio changed = current.changeGuests(guests);
        tabRodizios.updateGuests(changed);
        events.publish(new RodizioChanged(establishmentId, table.id(), tab.id(), clock.instant()));
        return TableRodizio.from(changed);
    }
}
