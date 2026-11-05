package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.KitchenItem;
import br.com.cardapio.application.port.in.order.OrderItemStep;
import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.in.order.WaitingOrder;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderItemNotFoundException;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.order.exception.OrderNotFoundException;
import br.com.cardapio.domain.order.exception.OrderStateException;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.support.InMemoryRodizios;
import br.com.cardapio.support.InMemorySubscriptions;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O outro lado do pedido: a fila da cozinha, os passos de cada item e a confirmação do garçom. */
class KitchenServicesTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();

    private static final Instant NOW = Instant.parse("2026-11-03T20:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";
    private static final PageQuery PAGE = new PageQuery(0, 50);

    private final OrderFakes fakes = new OrderFakes();
    private final InMemoryRodizios rodizios = new InMemoryRodizios();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final List<DomainEvent> published = new ArrayList<>();
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final UserId kenji = UserId.newId();
    private final UserId carlos = UserId.newId();
    private Product guioza;
    private Product sunomono;

    @BeforeEach
    void aTableWithAPhone() {
        DiningTable mesa04 = tables.given(sakura, "04", QR_04);
        sessions.givenPresent(sakura, mesa04.id(), "ana", NOW);
        guioza = fakes.product(sakura, "Guioza", 29_90, true);
        sunomono = fakes.product(sakura, "Sunomono", 19_90, true);
    }

    /** Um pedido da Ana, um minuto depois do anterior (a fila é por ordem de chegada). */
    private TableOrder place(OrderLine... lines) {
        clock = Clock.offset(clock, Duration.ofMinutes(1));
        return new PlaceOrderService(tables, sessions, fakes, fakes, fakes, fakes, rodizios, rodizios, subscriptions,
                published::add, clock)
                .place(new PlaceOrderCommand(QR_04, "ana", List.of(lines)));
    }

    private OrderLine line(Product product, int quantity) {
        return new OrderLine(product.id(), quantity, null);
    }

    private List<KitchenItem> queue(OrderItemStatus... statuses) {
        Set<OrderItemStatus> wanted = statuses.length == 0
                ? EnumSet.noneOf(OrderItemStatus.class) : EnumSet.of(statuses[0], statuses);
        return new ListKitchenQueueService(fakes).list(sakura, wanted, PAGE).content();
    }

    private void advance(OrderItemId id, OrderItemStep step) {
        new AdvanceOrderItemService(fakes, published::add, clock).advance(sakura, id, step, kenji);
    }

    private OrderItem item(OrderItemId id) {
        return fakes.items.get(id);
    }

    private ReviewOrderService review() {
        return new ReviewOrderService(fakes, published::add, clock);
    }

    @Test
    void theKitchenSeesTheItemsInArrivalOrder() {
        TableOrder first = place(line(guioza, 2), line(sunomono, 1));
        TableOrder second = place(line(sunomono, 3));

        assertThat(queue()).extracting(KitchenItem::productName).containsExactly("Guioza", "Sunomono", "Sunomono");
        assertThat(queue()).extracting(KitchenItem::orderNumber).containsExactly(1, 1, 2);
        advance(first.items().getFirst().id(), OrderItemStep.READY);
        assertThat(queue(OrderItemStatus.READY)).singleElement()
                .satisfies(i -> assertThat(i.id()).isEqualTo(first.items().getFirst().id()));
        assertThat(queue(OrderItemStatus.PENDING)).hasSize(2);
        assertThat(second.items()).hasSize(1);
        assertThatThrownBy(() -> queue(OrderItemStatus.DELIVERED)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theItemGoesFromTheQueueToTheTable() {
        OrderItemId id = place(line(guioza, 1)).items().getFirst().id();
        published.clear();

        advance(id, OrderItemStep.START);
        assertThat(item(id).status()).isEqualTo(OrderItemStatus.PREPARING);
        advance(id, OrderItemStep.READY);
        assertThat(item(id).status()).isEqualTo(OrderItemStatus.READY);
        new AdvanceOrderItemService(fakes, published::add, clock).advance(sakura, id, OrderItemStep.DELIVER, carlos);

        assertThat(item(id).status()).isEqualTo(OrderItemStatus.DELIVERED);
        assertThat(item(id).deliveredBy()).isEqualTo(carlos);
        assertThat(queue()).isEmpty();
        assertThat(published).hasSize(3).allMatch(e -> e instanceof OrderChanged);
        assertThatThrownBy(() -> advance(id, OrderItemStep.READY)).isInstanceOf(OrderItemStateException.class);
    }

    @Test
    void anItemFromAnotherRestaurantDoesNotExist() {
        OrderItemId id = place(line(guioza, 1)).items().getFirst().id();
        EstablishmentId other = EstablishmentId.newId();

        assertThatThrownBy(() -> new AdvanceOrderItemService(fakes, published::add, clock)
                .advance(other, id, OrderItemStep.START, kenji)).isInstanceOf(OrderItemNotFoundException.class);
        assertThatThrownBy(() -> new CancelOrderItemByStaffService(fakes, published::add, clock)
                .cancel(other, id, kenji, "Acabou")).isInstanceOf(OrderItemNotFoundException.class);
        assertThat(item(id).status()).isEqualTo(OrderItemStatus.PENDING);
    }

    @Test
    void theStaffCancelsWithAReason() {
        OrderItemId id = place(line(sunomono, 1)).items().getFirst().id();
        advance(id, OrderItemStep.START);

        new CancelOrderItemByStaffService(fakes, published::add, clock).cancel(sakura, id, kenji, "Acabou o pepino");

        assertThat(item(id).status()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(item(id).cancelReason()).isEqualTo("Acabou o pepino");
        assertThat(item(id).canceledBy()).isEqualTo(kenji);
        assertThat(queue()).isEmpty();
    }

    @Test
    void aWaitingOrderStaysOutOfTheKitchenUntilTheWaiterApproves() {
        fakes.settings.put(sakura, OrderSettings.defaults(sakura, NOW).update(true, true, null, null, NOW));
        TableOrder order = place(line(guioza, 2), line(sunomono, 1));
        OrderItemId guiozaId = order.items().getFirst().id();

        assertThat(queue()).isEmpty();
        assertThatThrownBy(() -> advance(guiozaId, OrderItemStep.START)).isInstanceOf(OrderStateException.class);
        List<WaitingOrder> waiting = new ListWaitingOrdersService(fakes).list(sakura, PAGE).content();
        assertThat(waiting).singleElement().satisfies(w -> {
            assertThat(w.number()).isEqualTo(1);
            assertThat(w.total()).isEqualTo(Money.ofCents(2 * 29_90 + 19_90));
        });

        review().approve(sakura, order.id(), carlos);

        assertThat(fakes.orders.get(order.id()).status()).isEqualTo(OrderStatus.SENT);
        assertThat(fakes.orders.get(order.id()).approvedBy()).isEqualTo(carlos);
        assertThat(queue()).hasSize(2);
        assertThat(new ListWaitingOrdersService(fakes).list(sakura, PAGE).content()).isEmpty();
        assertThatThrownBy(() -> review().approve(sakura, order.id(), carlos))
                .isInstanceOf(OrderStateException.class);
    }

    @Test
    void aRejectedOrderCancelsItsItemsAndLeavesTheBill() {
        fakes.settings.put(sakura, OrderSettings.defaults(sakura, NOW).update(true, true, null, null, NOW));
        TableOrder order = place(line(guioza, 1), line(sunomono, 1));

        review().reject(sakura, order.id(), carlos);

        assertThat(fakes.orders.get(order.id()).status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(fakes.itemsOf(order.id())).allSatisfy(i -> {
            assertThat(i.status()).isEqualTo(OrderItemStatus.CANCELED);
            assertThat(i.cancelReason()).isEqualTo(OrderItem.REJECTED_BY_STAFF);
        });
        assertThat(new ListTableOrdersService(tables, sessions, fakes, fakes, fakes, rodizios, rodizios, clock).list(QR_04, "ana")
                .total()).isEqualTo(Money.ZERO);
        assertThatThrownBy(() -> review().reject(EstablishmentId.newId(), order.id(), carlos))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
