package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.RecordMenuEventsUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.menu.MenuEventPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.menu.model.MenuEvent;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Grava os eventos do cardápio que o celular manda em lote (REL-27, 28, 29). Só com uma sessão válida
 * desta mesa; a hora é a do servidor. Uma sessão com eventos demais para de gravar, sem erro: o celular
 * não precisa saber, e o relatório não fica distorcido por quem fica martelando.
 */
public class RecordMenuEventsService implements RecordMenuEventsUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final MenuEventPort events;
    private final Clock clock;

    public RecordMenuEventsService(DiningTablePort tables, TableSessionPort sessions, MenuEventPort events,
                                   Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int record(String tableToken, String sessionToken, List<MenuEvent> batch) {
        if (batch.size() > MenuEvent.MAX_BATCH) {
            throw new InvalidValueException("Eventos demais num envio");
        }
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, now);
        if (batch.isEmpty()) {
            return 0;
        }
        int room = MenuEvent.MAX_PER_SESSION - events.countForSession(visit.session().id());
        if (room <= 0) {
            return 0;
        }
        List<MenuEvent> kept = batch.size() > room ? batch.subList(0, room) : batch;
        return events.save(visit.table().establishmentId(), visit.session().id(), kept, now);
    }
}
