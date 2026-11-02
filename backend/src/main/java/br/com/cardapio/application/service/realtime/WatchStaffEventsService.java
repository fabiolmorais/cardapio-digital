package br.com.cardapio.application.service.realtime;

import br.com.cardapio.application.port.in.realtime.LiveEvent;
import br.com.cardapio.application.port.in.realtime.WatchStaffEventsUseCase;
import br.com.cardapio.application.port.out.realtime.EventStreamPort;
import br.com.cardapio.domain.account.model.EstablishmentId;

import java.util.function.Consumer;

public class WatchStaffEventsService implements WatchStaffEventsUseCase {

    private final EventStreamPort events;

    public WatchStaffEventsService(EventStreamPort events) {
        this.events = events;
    }

    @Override
    public Runnable watch(EstablishmentId establishmentId, Consumer<LiveEvent> onEvent, Runnable onEnd) {
        return events.subscribe(establishmentId, event -> LiveEvent.forStaff(event).ifPresent(onEvent), onEnd);
    }
}
