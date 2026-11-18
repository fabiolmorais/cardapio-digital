package br.com.cardapio.domain.order.service;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.menu.model.ItemChoices;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** O que conta como "a mesa já pediu" (ADR 0011). */
class DuplicateOrderPolicyTest {

    private static final Instant ORDERED = Instant.parse("2026-11-18T20:00:00Z");
    private static final UserId WAITER = UserId.newId();

    private final EstablishmentId sakura = EstablishmentId.newId();
    private final Order order = Order.placeByStaff(sakura, TabId.newId(), DiningTableId.newId(), WAITER, 1,
            UUID.randomUUID(), false, ORDERED);
    private final Product chopp = Product.create(sakura, MenuCategoryId.newId(),
            new ProductDetails("Chopp", "", Money.ofCents(1200), null, false), true, 0);
    private final OrderLine line = new OrderLine(chopp.id(), 2, null);
    private final OrderItem item = OrderItem.from(order.id(), chopp, line, ORDERED);

    @Test
    void itemOnTheWayCountsHoweverOldItIs() {
        assertThat(DuplicateOrderPolicy.counts(order, item, ORDERED.plus(Duration.ofHours(1)))).isTrue();
    }

    @Test
    void deliveredItemCountsOnlyForFiveMinutes() {
        OrderItem delivered = item.start(ORDERED).markReady(ORDERED).deliver(WAITER, ORDERED);

        assertThat(DuplicateOrderPolicy.counts(order, delivered, ORDERED.plus(Duration.ofMinutes(4)))).isTrue();
        assertThat(DuplicateOrderPolicy.counts(order, delivered, ORDERED.plus(Duration.ofMinutes(5)))).isFalse();
    }

    @Test
    void canceledItemDoesNotCount() {
        assertThat(DuplicateOrderPolicy.counts(order, item.cancelByStaff(WAITER, "acabou", ORDERED), ORDERED))
                .isFalse();
    }

    @Test
    void rejectedOrderDoesNotCount() {
        Order waiting = Order.place(sakura, TabId.newId(), DiningTableId.newId(),
                TableSessionId.newId(), 2, true, ORDERED);

        assertThat(DuplicateOrderPolicy.counts(waiting, item, ORDERED)).isTrue();
        assertThat(DuplicateOrderPolicy.counts(waiting.reject(WAITER, ORDERED), item, ORDERED)).isFalse();
    }

    @Test
    void rodizioRoundDoesNotCount() {
        OrderItem round = OrderItem.inRound(order.id(), chopp, line, TabRodizioId.newId(), ItemChoices.NONE, ORDERED);

        assertThat(DuplicateOrderPolicy.counts(order, round, ORDERED)).isFalse();
    }
}
