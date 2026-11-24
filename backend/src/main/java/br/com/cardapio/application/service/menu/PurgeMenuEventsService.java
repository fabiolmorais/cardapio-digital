package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.PurgeMenuEventsUseCase;
import br.com.cardapio.application.port.out.menu.MenuEventPort;
import br.com.cardapio.domain.menu.model.MenuEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/** Apaga os eventos do cardápio com mais de 13 meses. */
public class PurgeMenuEventsService implements PurgeMenuEventsUseCase {

    private final MenuEventPort events;
    private final Clock clock;

    public PurgeMenuEventsService(MenuEventPort events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int purge() {
        return events.deleteBefore(clock.instant().minus(Duration.ofDays(MenuEvent.RETENTION_DAYS)));
    }
}
