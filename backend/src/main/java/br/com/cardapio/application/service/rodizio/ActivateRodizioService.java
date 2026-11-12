package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.ActivateRodizioCommand;
import br.com.cardapio.application.port.in.rodizio.ActivateRodizioUseCase;
import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.service.order.Tabs;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.rodizio.exception.RodizioAlreadyActiveException;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

public class ActivateRodizioService implements ActivateRodizioUseCase {

    private final DiningTablePort tables;
    private final RodizioPort rodizios;
    private final EstablishmentTimeZonePort timeZones;
    private final TabPort tabs;
    private final TabRodizioPort tabRodizios;
    private final CallPort calls;
    private final RodizioPhonePort phones;
    private final EventPublisherPort events;
    private final Clock clock;

    public ActivateRodizioService(DiningTablePort tables, RodizioPort rodizios, EstablishmentTimeZonePort timeZones,
                                  TabPort tabs, TabRodizioPort tabRodizios, CallPort calls, RodizioPhonePort phones,
                                  EventPublisherPort events, Clock clock) {
        this.phones = phones;
        this.tables = tables;
        this.rodizios = rodizios;
        this.timeZones = timeZones;
        this.tabs = tabs;
        this.tabRodizios = tabRodizios;
        this.calls = calls;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Tudo numa transação: a comanda abre (se a mesa ainda não pediu nada), o rodízio entra com o
     * preço de agora no relógio do restaurante, os celulares da mesa entram nele, e o "Quero o
     * rodízio" da mesa termina atendido.
     */
    @Override
    @Transactional
    public TableRodizio activate(ActivateRodizioCommand command) {
        var establishmentId = command.establishmentId();
        DiningTable table = tables.find(establishmentId, command.tableId()).filter(DiningTable::active)
                .orElseThrow(TableNotFoundException::new);
        Rodizio rodizio = rodizios.find(establishmentId, command.rodizioId()).filter(Rodizio::active)
                .orElseThrow(RodizioNotFoundException::new);
        Instant now = clock.instant();

        Tab tab = Tabs.openOrCreate(tabs, establishmentId, table.id(), now);
        tabRodizios.findByTab(establishmentId, tab.id()).ifPresent(r -> {
            throw new RodizioAlreadyActiveException(r.rodizioName());
        });
        TabRodizio activated = TabRodizio.activate(rodizio, tab.id(), command.guests(),
                LocalDateTime.ofInstant(now, timeZones.timeZoneOf(establishmentId)), command.by(), now);
        // Dois garçons ativando juntos: o índice único da comanda deixa valer um só.
        if (!tabRodizios.insertIfNone(activated)) {
            throw new RodizioAlreadyActiveException(rodizio.name());
        }
        // Os celulares que estão na mesa agora entram; quem chegar depois pergunta se participa.
        phones.joinAllAtTable(establishmentId, activated.id(), table.id(), now);

        calls.findOpen(establishmentId, table.id(), CallType.RODIZIO).ifPresent(open -> {
            Call done = open.resolveWithRodizio(command.by(), now);
            if (calls.update(done, open.status())) {
                events.publish(CallChanged.of(done, now));
            }
        });
        events.publish(new RodizioChanged(establishmentId, table.id(), tab.id(), now));
        return TableRodizio.from(activated);
    }
}
