package br.com.cardapio.application.service.realtime;

import br.com.cardapio.application.port.in.realtime.LiveEvent;
import br.com.cardapio.application.port.in.realtime.WatchTvEventsUseCase;
import br.com.cardapio.application.port.out.realtime.EventStreamPort;
import br.com.cardapio.application.service.device.DeviceAuthenticator;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

public class WatchTvEventsService implements WatchTvEventsUseCase {

    private final DeviceAuthenticator devices;
    private final EventStreamPort events;

    public WatchTvEventsService(DeviceAuthenticator devices, EventStreamPort events) {
        this.devices = devices;
        this.events = events;
    }

    /** A TV recebe o mesmo que a equipe: ela mostra a fila inteira do restaurante. */
    @Override
    @Transactional
    public Runnable watch(String deviceToken, Consumer<LiveEvent> onEvent, Runnable onEnd) {
        var establishmentId = devices.authenticateTv(deviceToken).device().establishmentId();
        return events.subscribe(establishmentId, event -> LiveEvent.forStaff(event).ifPresent(onEvent), onEnd);
    }
}
