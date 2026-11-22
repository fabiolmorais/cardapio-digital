package br.com.cardapio.application.service.order;

import br.com.cardapio.application.service.HistoryFakes;
import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.PlaceStaffOrderCommand;
import br.com.cardapio.application.port.in.order.StaffOrder;
import br.com.cardapio.application.port.in.order.StaffOrderSheet;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.exception.DuplicateOrderException;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.order.exception.StaffOrderingDisabledException;
import br.com.cardapio.domain.order.model.AlreadyOrdered;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.support.InMemoryProductOptions;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O garçom lança o pedido, e a trava segura o que a mesa já pediu (ADR 0011). */
class StaffOrderServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-18T20:00:00Z");
    private static final String QR = "qr-da-mesa-04-00000000000";

    private final OrderFakes fakes = new OrderFakes();
    private final HistoryFakes history = new HistoryFakes();
    private final InMemoryRodizios rodizios = new InMemoryRodizios();
    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final TablesByToken tables = new TablesByToken();
    private final List<DomainEvent> published = new ArrayList<>();
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final UserId joao = UserId.newId();
    private final UserId ana = UserId.newId();
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private DiningTable mesa;
    private Product chopp;
    private Product guioza;

    @BeforeEach
    void aRestaurantThatAllowsStaffOrders() {
        mesa = tables.given(sakura, "04", QR);
        chopp = fakes.product(sakura, "Chopp", 12_00, true);
        guioza = fakes.product(sakura, "Guioza", 29_90, true);
        fakes.staffNames.put(joao, "João");
        fakes.staffNames.put(ana, "Ana");
        allowStaffOrders(true);
    }

    private void allowStaffOrders(boolean allowed) {
        fakes.settings.put(sakura, OrderSettings.defaults(sakura, NOW)
                .update(true, true, allowed, null, null, NOW));
    }

    private PlaceStaffOrderService service() {
        return new PlaceStaffOrderService(tables, fakes, fakes, new InMemoryProductOptions(), fakes, fakes, rodizios,
                rodizios, subscriptions, published::add, history, clock);
    }

    private StaffOrder place(UserId by, Set<ProductId> confirmed, Product... products) {
        return place(UUID.randomUUID(), by, confirmed, products);
    }

    private StaffOrder place(UUID request, UserId by, Set<ProductId> confirmed, Product... products) {
        List<OrderLine> lines = List.of(products).stream().map(p -> new OrderLine(p.id(), 2, null)).toList();
        return service().place(new PlaceStaffOrderCommand(sakura, by, mesa.id(), request, lines, false, confirmed));
    }

    private void later(Duration elapsed) {
        clock = Clock.fixed(NOW.plus(elapsed), ZoneOffset.UTC);
    }

    private StaffOrderSheet sheet() {
        return new GetStaffOrderSheetService(tables, fakes, fakes, fakes, rodizios, rodizios, clock)
                .get(sakura, mesa.id());
    }

    @Test
    void theOrderOpensTheTabAndGoesStraightToTheKitchen() {
        StaffOrder placed = place(joao, Set.of(), chopp);

        Order order = fakes.orders.get(placed.id());
        assertThat(fakes.tabs).containsKey(placed.tabId());
        // A confirmação de todos os pedidos está ligada neste restaurante, e não vale para a equipe.
        assertThat(order.status()).isEqualTo(OrderStatus.SENT);
        assertThat(order.byStaff()).isTrue();
        assertThat(order.duplicateConfirmed()).isFalse();
        assertThat(fakes.locks).isEqualTo(1);
        assertThat(published).hasSize(1);
    }

    @Test
    void theRestaurantDecidesWhetherStaffCanOrder() {
        allowStaffOrders(false);

        assertThatThrownBy(() -> place(joao, Set.of(), chopp)).isInstanceOf(StaffOrderingDisabledException.class);
        assertThat(fakes.orders).isEmpty();
        assertThat(sheet().orderingEnabled()).isFalse();
    }

    @Test
    void aTableFromAnotherRestaurantDoesNotExist() {
        var command = new PlaceStaffOrderCommand(EstablishmentId.newId(), joao, mesa.id(), UUID.randomUUID(),
                List.of(new OrderLine(chopp.id(), 1, null)), false, Set.of());

        assertThatThrownBy(() -> service().place(command)).isInstanceOf(TableNotFoundException.class);
    }

    @Test
    void theSecondWaiterIsStoppedAndSeesWhoAlreadyOrdered() {
        place(joao, Set.of(), chopp);

        assertThatThrownBy(() -> place(ana, Set.of(), chopp, guioza))
                .isInstanceOfSatisfying(DuplicateOrderException.class, e -> {
                    assertThat(e.duplicates()).singleElement().satisfies(d -> {
                        assertThat(d.productName()).isEqualTo("Chopp");
                        assertThat(d.quantity()).isEqualTo(2);
                        assertThat(d.placedByName()).isEqualTo("João");
                        assertThat(d.status()).isEqualTo(OrderItemStatus.PENDING);
                    });
                });
        assertThat(fakes.orders).hasSize(1);
        // O aviso fica gravado para os relatórios, com o garçom avisado e o produto repetido.
        assertThat(history.warnings).singleElement().satisfies(w -> {
            assertThat(w.by()).isEqualTo(ana);
            assertThat(w.products()).containsExactly(chopp.id());
            assertThat(w.confirmed()).isFalse();
        });
    }

    @Test
    void confirmingSendsTheOrderAndRecordsIt() {
        place(joao, Set.of(), chopp);

        StaffOrder again = place(ana, Set.of(chopp.id()), chopp);

        assertThat(fakes.orders.get(again.id()).duplicateConfirmed()).isTrue();
        assertThat(fakes.orders).hasSize(2);
        assertThat(history.warnings).singleElement().satisfies(w -> {
            assertThat(w.products()).containsExactly(chopp.id());
            assertThat(w.confirmed()).isTrue();
        });
    }

    @Test
    void aConfirmationDoesNotCoverAnotherRepeatedProduct() {
        place(joao, Set.of(), chopp, guioza);

        assertThatThrownBy(() -> place(ana, Set.of(chopp.id()), chopp, guioza))
                .isInstanceOfSatisfying(DuplicateOrderException.class, e -> assertThat(e.duplicates())
                        .extracting(AlreadyOrdered::productName).containsExactly("Guioza"));
    }

    @Test
    void anOrderFromAPhoneAlsoCounts() {
        sessions.givenPresent(sakura, mesa.id(), "bia", NOW);
        new PlaceOrderService(tables, sessions, fakes, fakes, new InMemoryProductOptions(), fakes, fakes, rodizios,
                rodizios, rodizios, subscriptions, published::add, clock)
                .place(new PlaceOrderCommand(QR, "bia", List.of(new OrderLine(chopp.id(), 1, null))));

        assertThatThrownBy(() -> place(joao, Set.of(), chopp))
                .isInstanceOfSatisfying(DuplicateOrderException.class, e -> assertThat(e.duplicates())
                        .singleElement().satisfies(d -> {
                            assertThat(d.byStaff()).isFalse();
                            assertThat(d.placedByName()).isNull();
                        }));
    }

    @Test
    void aDeliveredItemStopsCountingAfterFiveMinutes() {
        StaffOrder first = place(joao, Set.of(), chopp);
        OrderItem item = fakes.itemsOf(first.id()).getFirst();
        fakes.items.put(item.id(), item.start(NOW).markReady(NOW).deliver(joao, NOW));

        later(Duration.ofMinutes(4));
        assertThatThrownBy(() -> place(ana, Set.of(), chopp)).isInstanceOf(DuplicateOrderException.class);

        later(Duration.ofMinutes(5));
        assertThat(fakes.orders.get(place(ana, Set.of(), chopp).id()).duplicateConfirmed()).isFalse();
    }

    @Test
    void repeatingTheSameRequestReturnsTheSameOrder() {
        UUID request = UUID.randomUUID();
        StaffOrder first = place(request, joao, Set.of(), chopp);

        StaffOrder repeated = place(request, joao, Set.of(), chopp);

        assertThat(repeated).isEqualTo(first);
        assertThat(fakes.orders).hasSize(1);
        assertThat(published).hasSize(1);
    }

    @Test
    void aRoundNeedsARodizioOnTheTable() {
        place(joao, Set.of(), guioza);
        var round = new PlaceStaffOrderCommand(sakura, joao, mesa.id(), UUID.randomUUID(),
                List.of(new OrderLine(guioza.id(), 1, null)), true, Set.of());

        assertThatThrownBy(() -> service().place(round)).isInstanceOf(NoRodizioOnTableException.class);
    }

    @Test
    void theSheetShowsWhatIsAlreadyOrdered() {
        place(joao, Set.of(), chopp);

        StaffOrderSheet sheet = sheet();

        assertThat(sheet.orderingEnabled()).isTrue();
        assertThat(sheet.tableLabel()).isEqualTo("04");
        assertThat(sheet.rodizio()).isNull();
        assertThat(sheet.alreadyOrdered()).extracting(AlreadyOrdered::placedByName).containsExactly("João");
    }

    @Test
    void theCustomerCannotCancelWhatTheWaiterOrdered() {
        sessions.givenPresent(sakura, mesa.id(), "bia", NOW);
        StaffOrder placed = place(joao, Set.of(), chopp);
        OrderItem item = fakes.itemsOf(placed.id()).getFirst();

        assertThatThrownBy(() -> new CancelOrderItemService(tables, sessions, fakes, published::add, clock)
                .cancel(QR, "bia", item.id()))
                .isInstanceOf(OrderItemStateException.class).hasMessageContaining("garçom");
    }
}
