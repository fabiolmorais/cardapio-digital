package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.in.rodizio.TurnRodizioCardUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class TurnRodizioCardService implements TurnRodizioCardUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final TabPort tabs;
    private final TabRodizioPort tabRodizios;
    private final EventPublisherPort events;
    private final Clock clock;

    public TurnRodizioCardService(DiningTablePort tables, TableSessionPort sessions, TabPort tabs,
                                  TabRodizioPort tabRodizios, EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.tabs = tabs;
        this.tabRodizios = tabRodizios;
        this.events = events;
        this.clock = clock;
    }

    /**
     * As travas de sempre da mesa (o celular bloqueado, a presença no salão) e o rodízio da comanda
     * aberta. Os passadores ficam sabendo pelo tempo real; a mesma cor de novo não avisa ninguém.
     */
    @Override
    @Transactional
    public TableRodizio turn(String tableToken, String sessionToken, TabRodizioStatus status) {
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, now);
        if (visit.session().isBlocked()) {
            throw new SessionBlockedException();
        }
        if (!visit.session().isPresent()) {
            throw new PresenceRequiredException();
        }
        var establishmentId = visit.table().establishmentId();
        Tab tab = tabs.findOpen(establishmentId, visit.table().id()).orElseThrow(NoRodizioOnTableException::new);
        TabRodizio current = tabRodizios.findByTab(establishmentId, tab.id())
                .orElseThrow(NoRodizioOnTableException::new);
        TabRodizio turned = current.turnCard(status, now);
        if (turned != current) {
            tabRodizios.updateStatus(turned);
            events.publish(new RodizioChanged(establishmentId, visit.table().id(), tab.id(), now));
        }
        return TableRodizio.from(turned);
    }
}
