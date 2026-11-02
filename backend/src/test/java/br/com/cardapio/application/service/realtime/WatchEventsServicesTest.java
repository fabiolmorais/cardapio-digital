package br.com.cardapio.application.service.realtime;

import br.com.cardapio.application.port.in.realtime.LiveEvent;
import br.com.cardapio.application.port.out.realtime.EventStreamPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.tablesession.exception.TableSessionRequiredException;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Quem assina o quê: a equipe, o restaurante inteiro; o celular, só a sua mesa. */
class WatchEventsServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";

    /** O fluxo de eventos na memória: emit() entrega aos assinantes do restaurante. */
    static final class FakeStream implements EventStreamPort {
        final Map<EstablishmentId, List<Consumer<DomainEvent>>> subscribers = new HashMap<>();

        @Override
        public Runnable subscribe(EstablishmentId establishmentId, Consumer<DomainEvent> onEvent, Runnable onEnd) {
            subscribers.computeIfAbsent(establishmentId, id -> new ArrayList<>()).add(onEvent);
            return () -> subscribers.get(establishmentId).remove(onEvent);
        }

        void emit(DomainEvent event) {
            List.copyOf(subscribers.getOrDefault(event.establishmentId(), List.of())).forEach(s -> s.accept(event));
        }
    }

    private final FakeStream stream = new FakeStream();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final List<LiveEvent> received = new ArrayList<>();
    private DiningTable mesa04;

    @BeforeEach
    void tableWithASession() {
        mesa04 = tables.given(sakura, "04", QR_04);
        sessions.given(sakura, mesa04.id(), "celular-04", NOW);
    }

    private static CallChanged call(EstablishmentId establishmentId, DiningTableId tableId) {
        return new CallChanged(establishmentId, tableId, CallId.newId(), CallType.WAITER, CallStatus.PENDING, NOW);
    }

    @Test
    void theTeamGetsEveryCallOfItsRestaurantAndOnlyIds() {
        new WatchStaffEventsService(stream).watch(sakura, received::add, () -> { });
        CallChanged event = call(sakura, mesa04.id());

        stream.emit(event);
        stream.emit(call(EstablishmentId.newId(), DiningTableId.newId()));

        assertThat(received).singleElement().satisfies(live -> {
            assertThat(live.name()).isEqualTo("call");
            assertThat(live.data()).containsOnlyKeys("callId", "tableId", "type", "status")
                    .containsEntry("callId", event.callId().value().toString())
                    .containsEntry("status", "PENDING");
        });
    }

    @Test
    void afterStoppingNothingArrives() {
        Runnable stop = new WatchStaffEventsService(stream).watch(sakura, received::add, () -> { });

        stop.run();
        stream.emit(call(sakura, mesa04.id()));

        assertThat(received).isEmpty();
    }

    @Test
    void thePhoneGetsOnlyTheCallsOfItsTable() {
        new WatchTableEventsService(tables, sessions, stream, clock)
                .watch(QR_04, "celular-04", received::add, () -> { });

        stream.emit(call(sakura, DiningTableId.newId())); // outra mesa do mesmo restaurante
        stream.emit(call(sakura, mesa04.id()));

        assertThat(received).singleElement().satisfies(live -> {
            assertThat(live.name()).isEqualTo("call");
            assertThat(live.data()).containsOnlyKeys("callId", "type", "status");
        });
    }

    @Test
    void withoutTheSessionThePhoneDoesNotSubscribe() {
        var service = new WatchTableEventsService(tables, sessions, stream, clock);

        assertThatThrownBy(() -> service.watch(QR_04, "inventado", received::add, () -> { }))
                .isInstanceOf(TableSessionRequiredException.class);
        assertThat(stream.subscribers).isEmpty();
    }
}
