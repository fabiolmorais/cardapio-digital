package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.ChangeRodizioGuestsUseCase;
import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallType;
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
import java.time.Instant;
import java.util.Map;

public class ChangeRodizioGuestsService implements ChangeRodizioGuestsUseCase {

    private final DiningTablePort tables;
    private final TabPort tabs;
    private final TabRodizioPort tabRodizios;
    private final RodizioPhonePort phones;
    private final CallPort calls;
    private final EventPublisherPort events;
    private final Clock clock;

    public ChangeRodizioGuestsService(DiningTablePort tables, TabPort tabs, TabRodizioPort tabRodizios,
                                      RodizioPhonePort phones, CallPort calls, EventPublisherPort events,
                                      Clock clock) {
        this.phones = phones;
        this.calls = calls;
        this.tables = tables;
        this.tabs = tabs;
        this.tabRodizios = tabRodizios;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Só na comanda aberta: depois da conta, o que foi cobrado não muda. A mesa de outro restaurante
     * é "não encontrada" (404), e não "sem rodízio": a resposta não conta nada da mesa dos outros.
     *
     * Quem chegou depois e disse "Vou participar" entra no rodízio aqui (o garçom contou a pessoa),
     * e o "Quero o rodízio" aberto da mesa termina atendido.
     */
    @Override
    @Transactional
    public TableRodizio change(EstablishmentId establishmentId, DiningTableId tableId, Map<String, Integer> guests,
                               UserId by) {
        DiningTable table = tables.find(establishmentId, tableId).orElseThrow(TableNotFoundException::new);
        Tab tab = tabs.findOpen(establishmentId, table.id()).orElseThrow(NoRodizioOnTableException::new);
        TabRodizio current = tabRodizios.findByTab(establishmentId, tab.id())
                .orElseThrow(NoRodizioOnTableException::new);
        TabRodizio changed = current.changeGuests(guests);
        tabRodizios.updateGuests(changed);
        Instant now = clock.instant();
        phones.admitRequested(establishmentId, changed.id(), now);
        calls.findOpen(establishmentId, table.id(), CallType.RODIZIO).ifPresent(open -> {
            Call done = open.resolveWithRodizio(by, now);
            if (calls.update(done, open.status())) {
                events.publish(CallChanged.of(done, now));
            }
        });
        events.publish(new RodizioChanged(establishmentId, table.id(), tab.id(), now));
        return TableRodizio.from(changed);
    }
}
