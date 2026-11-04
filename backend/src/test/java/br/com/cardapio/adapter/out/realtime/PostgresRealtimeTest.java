package br.com.cardapio.adapter.out.realtime;

import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.realtime.EventStreamPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.event.TabClosed;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O LISTEN/NOTIFY com o Postgres de verdade: o aviso só sai no commit, nunca num rollback,
 * e só chega a quem assinou aquele restaurante.
 */
@IntegrationTest
class PostgresRealtimeTest {

    @Autowired
    EventPublisherPort publisher;

    @Autowired
    EventStreamPort stream;

    @Autowired
    TransactionTemplate transaction;

    private final EstablishmentId sakura = EstablishmentId.newId();

    private CallChanged event() {
        return new CallChanged(sakura, DiningTableId.newId(), CallId.newId(), CallType.BILL, CallStatus.PENDING,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    private BlockingQueue<DomainEvent> listen(EstablishmentId establishmentId) {
        BlockingQueue<DomainEvent> queue = new LinkedBlockingQueue<>();
        stream.subscribe(establishmentId, queue::add, () -> { });
        return queue;
    }

    @Test
    void theEventArrivesOnlyAfterTheCommitAndOnlyForItsRestaurant() throws Exception {
        BlockingQueue<DomainEvent> mine = listen(sakura);
        BlockingQueue<DomainEvent> theirs = listen(EstablishmentId.newId());
        CallChanged event = event();

        transaction.executeWithoutResult(status -> {
            publisher.publish(event);
            try {
                // Ainda dentro da transação: o Postgres segura o aviso até o commit.
                assertThat(mine.poll(500, TimeUnit.MILLISECONDS)).isNull();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(mine.poll(5, TimeUnit.SECONDS)).isEqualTo(event); // ida e volta pelo JSON, igualzinho
        assertThat(theirs.poll(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void aRolledBackChangeWarnsNobody() throws Exception {
        BlockingQueue<DomainEvent> mine = listen(sakura);

        transaction.executeWithoutResult(status -> {
            publisher.publish(event());
            status.setRollbackOnly();
        });

        assertThat(mine.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void afterUnsubscribingNothingArrives() throws Exception {
        BlockingQueue<DomainEvent> queue = new LinkedBlockingQueue<>();
        Runnable stop = stream.subscribe(sakura, queue::add, () -> { });
        stop.run();

        transaction.executeWithoutResult(status -> publisher.publish(event()));

        assertThat(queue.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void anOrderEventTravelsTheSameWay() throws Exception {
        BlockingQueue<DomainEvent> mine = listen(sakura);
        OrderChanged order = new OrderChanged(sakura, DiningTableId.newId(), OrderId.newId(),
                OrderStatus.PENDING_APPROVAL, Instant.now().truncatedTo(ChronoUnit.MICROS));

        transaction.executeWithoutResult(status -> publisher.publish(order));

        assertThat(mine.poll(5, TimeUnit.SECONDS)).isEqualTo(order);
    }

    @Test
    void aClosedTabTravelsTheSameWay() throws Exception {
        BlockingQueue<DomainEvent> mine = listen(sakura);
        TabClosed closed = new TabClosed(sakura, DiningTableId.newId(), TabId.newId(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));

        transaction.executeWithoutResult(status -> publisher.publish(closed));

        assertThat(mine.poll(5, TimeUnit.SECONDS)).isEqualTo(closed);
    }

    @Test
    void aRodizioChangeTravelsTheSameWay() throws Exception {
        BlockingQueue<DomainEvent> mine = listen(sakura);
        RodizioChanged changed = new RodizioChanged(sakura, DiningTableId.newId(), TabId.newId(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));

        transaction.executeWithoutResult(status -> publisher.publish(changed));

        assertThat(mine.poll(5, TimeUnit.SECONDS)).isEqualTo(changed);
    }
}
