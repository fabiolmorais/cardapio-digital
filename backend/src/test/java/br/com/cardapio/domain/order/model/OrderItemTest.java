package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    private static final Instant NOW = Instant.parse("2026-11-03T20:00:00Z");
    private final Product sunomono = Product.create(EstablishmentId.newId(), MenuCategoryId.newId(),
            new ProductDetails("Sunomono", "", Money.ofCents(19_90), Money.ofCents(15_90), false), true, 0);

    private OrderItem item(int quantity, String note) {
        return OrderItem.from(OrderId.newId(), sunomono, new OrderLine(sunomono.id(), quantity, note), NOW);
    }

    @Test
    void theNameAndPriceAreCopiedFromTheMenuNow() {
        OrderItem item = item(2, "  sem gergelim ");

        assertThat(item.productName()).isEqualTo("Sunomono");
        assertThat(item.unitPrice()).isEqualTo(Money.ofCents(15_90)); // o da promoção
        assertThat(item.subtotal()).isEqualTo(Money.ofCents(31_80));
        assertThat(item.note()).isEqualTo("sem gergelim");
        assertThat(item.status()).isEqualTo(OrderItemStatus.PENDING);
    }

    @Test
    void theCustomerCancelsOnlyWhileItIsInTheQueue() {
        OrderItem canceled = item(1, null).cancelByCustomer(NOW.plusSeconds(30));

        assertThat(canceled.status()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(canceled.canceledAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(canceled.canceledBy()).isNull();
        assertThat(canceled.cancelReason()).isEqualTo(OrderItem.CANCELED_BY_CUSTOMER);
        assertThatThrownBy(() -> canceled.cancelByCustomer(NOW)).isInstanceOf(OrderItemStateException.class);

        OrderItem preparing = new OrderItem(canceled.id(), canceled.establishmentId(), canceled.orderId(),
                canceled.productId(), "Sunomono", Money.ofCents(15_90), 1, null, OrderItemStatus.PREPARING, NOW, NOW,
                null, null, null, null, null, null, null);
        assertThatThrownBy(() -> preparing.cancelByCustomer(NOW))
                .isInstanceOf(OrderItemStateException.class).hasMessageContaining("fale com o garçom");
    }

    @Test
    void aLineHasLimits() {
        assertThatThrownBy(() -> new OrderLine(sunomono.id(), 0, null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new OrderLine(sunomono.id(), 21, null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new OrderLine(sunomono.id(), 1, "x".repeat(141)))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new OrderLine(sunomono.id(), 1, "   ").note()).isNull();
    }

    @Test
    void theKitchenAndTheWaiterTakeTheItemToTheTable() {
        UserId carlos = UserId.newId();
        OrderItem preparing = item(1, null).start(NOW.plusSeconds(60));
        OrderItem ready = preparing.markReady(NOW.plusSeconds(600));
        OrderItem delivered = ready.deliver(carlos, NOW.plusSeconds(660));

        assertThat(preparing.status()).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(preparing.preparingAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(ready.readyAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(delivered.status()).isEqualTo(OrderItemStatus.DELIVERED);
        assertThat(delivered.deliveredBy()).isEqualTo(carlos);
        assertThat(delivered.deliveredAt()).isEqualTo(NOW.plusSeconds(660));
        // Os passos não voltam, e o entregue não sai de cena.
        assertThatThrownBy(() -> ready.start(NOW)).isInstanceOf(OrderItemStateException.class);
        assertThatThrownBy(() -> delivered.markReady(NOW)).isInstanceOf(OrderItemStateException.class);
        assertThatThrownBy(() -> delivered.deliver(carlos, NOW)).isInstanceOf(OrderItemStateException.class);
    }

    @Test
    void thePrintedTicketPutsTheQueuedItemInPreparation() {
        OrderItem printed = item(1, null).printedForKitchen(NOW.plusSeconds(30));
        assertThat(printed.status()).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(printed.preparingAt()).isEqualTo(NOW.plusSeconds(30));

        // Quem já tinha andado (pelo tablet) fica como está, sem erro.
        OrderItem ready = item(1, null).markReady(NOW);
        assertThat(ready.printedForKitchen(NOW.plusSeconds(30))).isEqualTo(ready);
        OrderItem preparing = item(1, null).start(NOW);
        assertThat(preparing.printedForKitchen(NOW.plusSeconds(30))).isEqualTo(preparing);
    }

    @Test
    void aDrinkCanSkipStraightToReadyOrDelivered() {
        assertThat(item(1, null).markReady(NOW).status()).isEqualTo(OrderItemStatus.READY);
        assertThat(item(1, null).markReady(NOW).preparingAt()).isNull();
        assertThat(item(1, null).deliver(UserId.newId(), NOW).status()).isEqualTo(OrderItemStatus.DELIVERED);
    }

    @Test
    void theStaffCancelsWithAReasonTheCustomerSees() {
        UserId kenji = UserId.newId();
        OrderItem canceled = item(1, null).start(NOW).cancelByStaff(kenji, "  Acabou o pepino  ", NOW);

        assertThat(canceled.status()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(canceled.canceledBy()).isEqualTo(kenji);
        assertThat(canceled.cancelReason()).isEqualTo("Acabou o pepino");
        assertThatThrownBy(() -> item(1, null).cancelByStaff(kenji, "  ", NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> item(1, null).cancelByStaff(kenji, "x".repeat(141), NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> canceled.cancelByStaff(kenji, "de novo", NOW))
                .isInstanceOf(OrderItemStateException.class);
        assertThatThrownBy(() -> item(1, null).deliver(kenji, NOW).cancelByStaff(kenji, "tarde", NOW))
                .isInstanceOf(OrderItemStateException.class).hasMessage("Este item já foi entregue");
    }

    @Test
    void theKitchenCancelsOnlyPartOfTheItem() {
        UserId kenji = UserId.newId();
        OrderItem ten = item(10, "sem gergelim").start(NOW);

        OrderItem.StaffCancel result = ten.cancelByStaff(kenji, "Só tem pepino para 5", 5, NOW.plusSeconds(60));

        assertThat(result.kept().id()).isEqualTo(ten.id());
        assertThat(result.kept().quantity()).isEqualTo(5);
        assertThat(result.kept().status()).isEqualTo(OrderItemStatus.PREPARING);
        assertThat(result.canceled().id()).isNotEqualTo(ten.id());
        assertThat(result.canceled().quantity()).isEqualTo(5);
        assertThat(result.canceled().status()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(result.canceled().cancelReason()).isEqualTo("Só tem pepino para 5");
        assertThat(result.canceled().canceledBy()).isEqualTo(kenji);
        assertThat(result.canceled().note()).isEqualTo("sem gergelim");
        assertThat(result.canceled().canceledByCustomer()).isFalse();
    }

    @Test
    void cancelingTheWholeQuantityIsTheUsualCancel() {
        OrderItem two = item(2, null);

        OrderItem.StaffCancel result = two.cancelByStaff(UserId.newId(), "Acabou", 2, NOW);

        assertThat(result.kept()).isNull();
        assertThat(result.canceled().id()).isEqualTo(two.id());
        assertThat(result.canceled().quantity()).isEqualTo(2);
        assertThatThrownBy(() -> two.cancelByStaff(UserId.newId(), "Acabou", 3, NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> two.cancelByStaff(UserId.newId(), "Acabou", 0, NOW))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void itKnowsWhenTheCustomerCanceled() {
        assertThat(item(1, null).cancelByCustomer(NOW).canceledByCustomer()).isTrue();
        assertThat(item(1, null).cancelByStaff(UserId.newId(), "Acabou", NOW).canceledByCustomer()).isFalse();
        assertThat(item(1, null).canceledByCustomer()).isFalse();
    }
}
