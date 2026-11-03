package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.in.order.TableOrders;
import br.com.cardapio.application.port.in.order.UpdateOrderSettingsCommand;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderItemNotFoundException;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.order.exception.OrderingDisabledException;
import br.com.cardapio.domain.order.exception.ProductUnavailableException;
import br.com.cardapio.domain.order.exception.TooManyOrdersException;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O pedido pelo celular: enviar, juntar na comanda, ver e cancelar. */
class OrderServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-03T20:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";
    private static final String QR_05 = "qr-da-mesa-05-00000000000";

    private final OrderFakes fakes = new OrderFakes();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final List<DomainEvent> published = new ArrayList<>();
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private DiningTable mesa04;
    private Product guioza;
    private Product sunomono;

    @BeforeEach
    void aTableWithThreePhones() {
        mesa04 = tables.given(sakura, "04", QR_04);
        DiningTable mesa05 = tables.given(sakura, "05", QR_05);
        for (String phone : new String[]{"ana", "bia", "caio"}) {
            sessions.givenPresent(sakura, mesa04.id(), phone, NOW);
        }
        sessions.givenPresent(sakura, mesa05.id(), "mesa-05", NOW);
        guioza = fakes.product(sakura, "Guioza", 29_90, true);
        sunomono = fakes.product(sakura, "Sunomono", 19_90, true);
    }

    private TableOrder place(String qr, String phone, OrderLine... lines) {
        return new PlaceOrderService(tables, sessions, fakes, fakes, fakes, fakes, published::add, clock)
                .place(new PlaceOrderCommand(qr, phone, List.of(lines)));
    }

    private TableOrder place(String phone, OrderLine... lines) {
        return place(QR_04, phone, lines);
    }

    private TableOrders list(String phone) {
        return new ListTableOrdersService(tables, sessions, fakes, fakes, fakes, clock).list(QR_04, phone);
    }

    private OrderLine line(Product product, int quantity) {
        return new OrderLine(product.id(), quantity, null);
    }

    @Test
    void threePhonesShareTheSameTab() {
        place("ana", line(guioza, 1));
        place("bia", line(sunomono, 2));
        place("caio", line(guioza, 1), line(sunomono, 1));

        assertThat(fakes.tabs).hasSize(1);
        TableOrders tab = list("bia");
        assertThat(tab.orders()).extracting(TableOrder::number).containsExactly(1, 2, 3);
        assertThat(tab.orders()).extracting(TableOrder::mine).containsExactly(false, true, false);
        assertThat(tab.total()).isEqualTo(Money.ofCents(29_90 + 2 * 19_90 + 29_90 + 19_90));
        assertThat(published).hasSize(3).allMatch(e -> e instanceof OrderChanged);
    }

    @Test
    void thePriceIsCopiedAtTheMomentOfTheOrder() {
        TableOrder order = place("ana", line(guioza, 2));

        assertThat(order.status()).isEqualTo(OrderStatus.SENT);
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("Guioza");
            assertThat(item.unitPrice()).isEqualTo(Money.ofCents(29_90));
            assertThat(item.status()).isEqualTo(OrderItemStatus.PENDING);
        });
    }

    @Test
    void aBigOrderWaitsForTheWaiter() {
        TableOrder many = place("ana", line(sunomono, 11)); // 11 itens: passa do limite de 10

        assertThat(many.status()).isEqualTo(OrderStatus.PENDING_APPROVAL);
    }

    @Test
    void aSoldOutOrUnknownProductStopsTheWholeOrder() {
        Product ceviche = fakes.product(sakura, "Ceviche", 34_90, false);
        Product theirs = fakes.product(EstablishmentId.newId(), "Do outro bar", 10_00, true);

        assertThatThrownBy(() -> place("ana", line(guioza, 1), line(ceviche, 1)))
                .isInstanceOf(ProductUnavailableException.class).hasMessageContaining("Ceviche");
        assertThatThrownBy(() -> place("ana", line(theirs, 1))).isInstanceOf(ProductUnavailableException.class);
        assertThat(fakes.orders).isEmpty();
        assertThat(fakes.tabs).isEmpty();
    }

    @Test
    void theUsualLocksApply() {
        sessions.given(sakura, mesa04.id(), "de-fora", NOW);
        sessions.save(sessions.byToken("caio").block(UserId.newId(), NOW));

        assertThatThrownBy(() -> place("de-fora", line(guioza, 1))).isInstanceOf(PresenceRequiredException.class);
        assertThatThrownBy(() -> place("caio", line(guioza, 1))).isInstanceOf(SessionBlockedException.class);
        assertThatThrownBy(() -> place("ana")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theRestaurantCanTurnOrderingOff() {
        new UpdateOrderSettingsService(fakes, clock)
                .update(new UpdateOrderSettingsCommand(sakura, false, false, null, null));

        assertThatThrownBy(() -> place("ana", line(guioza, 1))).isInstanceOf(OrderingDisabledException.class);
        assertThat(list("ana").orderingEnabled()).isFalse();
    }

    @Test
    void sendingWithoutStopHitsTheLimit() {
        for (int i = 0; i < PlaceOrderService.MAX_PER_MINUTE; i++) {
            place("ana", line(guioza, 1));
        }

        assertThatThrownBy(() -> place("ana", line(guioza, 1))).isInstanceOf(TooManyOrdersException.class);
        clock = Clock.offset(clock, Duration.ofMinutes(1));
        assertThat(place("ana", line(guioza, 1)).number()).isEqualTo(PlaceOrderService.MAX_PER_MINUTE + 1);
    }

    @Test
    void theCustomerCancelsOnlyHisOwnItemsWhileQueued() {
        var item = place("ana", line(guioza, 1)).items().getFirst().id();
        var cancel = new CancelOrderItemService(tables, sessions, fakes, published::add, clock);

        assertThatThrownBy(() -> cancel.cancel(QR_04, "bia", item)).isInstanceOf(OrderItemStateException.class);
        assertThatThrownBy(() -> cancel.cancel(QR_05, "mesa-05", item)).isInstanceOf(OrderItemNotFoundException.class);
        cancel.cancel(QR_04, "ana", item);

        assertThat(fakes.items.get(item).status()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(list("ana").total()).isEqualTo(Money.ZERO);
        assertThatThrownBy(() -> cancel.cancel(QR_04, "ana", item)).isInstanceOf(OrderItemStateException.class);
    }

    @Test
    void aTableWithoutOrdersHasAnEmptyTab() {
        TableOrders empty = list("ana");

        assertThat(empty.orderingEnabled()).isTrue();
        assertThat(empty.orders()).isEmpty();
        assertThat(empty.total()).isEqualTo(Money.ZERO);
    }
}
