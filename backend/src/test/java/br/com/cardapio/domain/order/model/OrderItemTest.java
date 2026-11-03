package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
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
                null, null, null, null, null, null);
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
}
