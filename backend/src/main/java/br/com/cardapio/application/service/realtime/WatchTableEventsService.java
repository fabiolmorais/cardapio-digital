package br.com.cardapio.application.service.realtime;

import br.com.cardapio.application.port.in.realtime.LiveEvent;
import br.com.cardapio.application.port.in.realtime.WatchTableEventsUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.realtime.EventStreamPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.event.TableEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.function.Consumer;

public class WatchTableEventsService implements WatchTableEventsUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final EventStreamPort events;
    private final Clock clock;

    public WatchTableEventsService(DiningTablePort tables, TableSessionPort sessions, EventStreamPort events,
                                   Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Confere a sessão (401 sem ela) e assina os eventos do restaurante da mesa, deixando passar
     * só os desta mesa. A transação é só para a conferência: a assinatura continua depois dela.
     */
    @Override
    @Transactional
    public Runnable watch(String tableToken, String sessionToken, Consumer<LiveEvent> onEvent, Runnable onEnd) {
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, clock.instant());
        DiningTableId tableId = visit.table().id();
        return events.subscribe(visit.table().establishmentId(), event -> {
            if (event instanceof TableEvent tableEvent && tableEvent.tableId().equals(tableId)) {
                LiveEvent.forTable(event).ifPresent(onEvent);
            }
        }, onEnd);
    }
}
