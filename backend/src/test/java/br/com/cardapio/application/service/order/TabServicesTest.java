package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ClosedTab;
import br.com.cardapio.application.port.in.order.OpenTab;
import br.com.cardapio.application.port.in.order.OrderItemStep;
import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.service.call.InMemoryCalls;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.order.event.TabClosed;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeriod;
import br.com.cardapio.domain.order.model.TabStatus;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.support.InMemoryRodizios;
import br.com.cardapio.support.InMemorySubscriptions;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A comanda do lado da equipe: as abertas, o detalhe e o fechamento depois do pagamento. */
class TabServicesTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();

    private static final Instant NOW = Instant.parse("2026-11-03T22:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";

    private final OrderFakes fakes = new OrderFakes();
    private final InMemoryRodizios rodizios = new InMemoryRodizios();
    private final InMemoryCalls calls = new InMemoryCalls();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final List<DomainEvent> published = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final UserId carlos = UserId.newId();
    private DiningTable mesa04;
    private Product guioza;
    private Product sunomono;

    @BeforeEach
    void aTableWithTwoPhones() {
        mesa04 = tables.given(sakura, "04", QR_04);
        sessions.givenPresent(sakura, mesa04.id(), "ana", NOW);
        sessions.givenPresent(sakura, mesa04.id(), "bia", NOW);
        guioza = fakes.product(sakura, "Guioza", 29_90, true);
        sunomono = fakes.product(sakura, "Sunomono", 19_90, true);
    }

    private TableOrder place(String phone, Product product, int quantity) {
        return new PlaceOrderService(tables, sessions, fakes, fakes, fakes, fakes, rodizios, rodizios, subscriptions,
                published::add, clock)
                .place(new PlaceOrderCommand(QR_04, phone, List.of(new OrderLine(product.id(), quantity, null))));
    }

    private void deliverAll(TableOrder order) {
        order.items().forEach(i -> new AdvanceOrderItemService(fakes, published::add, clock)
                .advance(sakura, i.id(), OrderItemStep.DELIVER, carlos));
    }

    private TabId theTab() {
        return fakes.tabs.values().iterator().next().id();
    }

    private StaffTab close(PaymentMethod method) {
        return new CloseTabService(fakes, fakes, calls, sessions, rodizios, published::add, clock)
                .close(sakura, theTab(), carlos, method);
    }

    @Test
    void theOpenTabsShowTheTotalAndWhatIsStillOnTheWay() {
        TableOrder first = place("ana", guioza, 2);
        place("bia", sunomono, 1);
        deliverAll(first);

        List<OpenTab> open = new ListOpenTabsService(fakes).list(sakura, new PageQuery(0, 50)).content();

        assertThat(open).singleElement().satisfies(tab -> {
            assertThat(tab.total()).isEqualTo(Money.ofCents(2 * 29_90 + 19_90));
            assertThat(tab.itemsOnTheWay()).isEqualTo(1);
            assertThat(tab.tableId()).isEqualTo(mesa04.id());
        });
    }

    @Test
    void theTabShowsAllOrdersAndTheBillTheTableAskedFor() {
        place("ana", guioza, 2);
        place("bia", sunomono, 1);
        calls.insertIfNoneOpen(Call.open(sakura, mesa04.id(), sessions.byToken("ana").id(), CallType.BILL,
                PaymentMethod.CARD, NOW));

        StaffTab tab = new GetTabService(fakes, fakes, calls, rodizios).get(sakura, theTab());

        assertThat(tab.orders()).hasSize(2);
        assertThat(tab.total()).isEqualTo(Money.ofCents(2 * 29_90 + 19_90));
        assertThat(tab.billRequestedWith()).isEqualTo(PaymentMethod.CARD);
        assertThat(tab.status()).isEqualTo(TabStatus.OPEN);
        assertThatThrownBy(() -> new GetTabService(fakes, fakes, calls, rodizios).get(EstablishmentId.newId(), theTab()))
                .isInstanceOf(TabNotFoundException.class);
    }

    @Test
    void closingEndsTheVisitAndTheOpenCalls() {
        deliverAll(place("ana", guioza, 2));
        Call bill = Call.open(sakura, mesa04.id(), sessions.byToken("ana").id(), CallType.BILL, PaymentMethod.PIX,
                NOW);
        calls.insertIfNoneOpen(bill);
        published.clear();

        StaffTab closed = close(PaymentMethod.PIX);

        assertThat(closed.status()).isEqualTo(TabStatus.CLOSED);
        assertThat(closed.total()).isEqualTo(Money.ofCents(2 * 29_90));
        assertThat(closed.paymentMethod()).isEqualTo(PaymentMethod.PIX);
        Tab saved = fakes.tabs.get(theTab());
        assertThat(saved.closedBy()).isEqualTo(carlos);
        assertThat(sessions.byToken("ana").endedAt()).isEqualTo(NOW);
        assertThat(sessions.byToken("bia").endedAt()).isEqualTo(NOW);
        assertThat(calls.saved.get(bill.id()).status()).isEqualTo(CallStatus.DONE);
        assertThat(published).hasSize(2);
        assertThat(published.get(0)).isInstanceOf(CallChanged.class);
        assertThat(published.get(1)).isInstanceOf(TabClosed.class);
        assertThat(new ListOpenTabsService(fakes).list(sakura, new PageQuery(0, 50)).content()).isEmpty();
        assertThatThrownBy(() -> close(PaymentMethod.PIX)).isInstanceOf(TabStateException.class);
    }

    @Test
    void itDoesNotCloseWhileSomethingIsOnTheWay() {
        place("ana", guioza, 1);

        assertThatThrownBy(() -> close(PaymentMethod.CASH))
                .isInstanceOf(TabStateException.class).hasMessageContaining("1× Guioza");
        assertThat(fakes.tabs.get(theTab()).status()).isEqualTo(TabStatus.OPEN);
        assertThat(sessions.byToken("ana").endedAt()).isNull();
    }

    @Test
    void theHistoryHasTheClosedTabsOfThePeriod() {
        deliverAll(place("ana", guioza, 2));
        TabId closed = theTab();
        close(PaymentMethod.CASH);
        // A visita seguinte (um celular novo: os da mesa saíram) abre outra comanda, que não entra.
        sessions.givenPresent(sakura, mesa04.id(), "caio", NOW);
        place("caio", sunomono, 1);
        LocalDate today = LocalDate.parse("2026-11-03");
        ListClosedTabsService history = new ListClosedTabsService(fakes);

        List<ClosedTab> found = history.list(sakura, new TabPeriod(today, today), null, new PageQuery(0, 50))
                .content();

        assertThat(found).singleElement().satisfies(tab -> {
            assertThat(tab.id()).isEqualTo(closed);
            assertThat(tab.total()).isEqualTo(Money.ofCents(2 * 29_90));
            assertThat(tab.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        });
        assertThat(history.list(sakura, new TabPeriod(today.minusDays(7), today.minusDays(1)), null,
                new PageQuery(0, 50)).content()).isEmpty();
        assertThat(history.list(EstablishmentId.newId(), new TabPeriod(today, today), null, new PageQuery(0, 50))
                .content()).isEmpty();
    }
}
