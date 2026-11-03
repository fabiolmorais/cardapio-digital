package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabTest {

    private static final Instant NOW = Instant.parse("2026-11-03T22:00:00Z");
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final UserId carlos = UserId.newId();
    private final Tab tab = Tab.open(sakura, DiningTableId.newId(), NOW.minusSeconds(3600));

    private OrderItem item(String name, long priceCents, int quantity) {
        Product product = Product.create(sakura, MenuCategoryId.newId(),
                new ProductDetails(name, "", Money.ofCents(priceCents), null, false), true, 0);
        return OrderItem.from(OrderId.newId(), product, new OrderLine(product.id(), quantity, null), NOW);
    }

    @Test
    void closingRecordsWhatWasChargedAndHow() {
        OrderItem guioza = item("Guioza", 29_90, 2).deliver(carlos, NOW);
        OrderItem sunomono = item("Sunomono", 19_90, 1).cancelByCustomer(NOW);

        Tab closed = tab.close(carlos, PaymentMethod.PIX, List.of(guioza, sunomono), NOW);

        assertThat(closed.status()).isEqualTo(TabStatus.CLOSED);
        assertThat(closed.total()).isEqualTo(Money.ofCents(2 * 29_90)); // o cancelado não entra
        assertThat(closed.paymentMethod()).isEqualTo(PaymentMethod.PIX);
        assertThat(closed.closedBy()).isEqualTo(carlos);
        assertThat(closed.closedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> closed.close(carlos, PaymentMethod.CASH, List.of(), NOW))
                .isInstanceOf(TabStateException.class).hasMessage("Esta comanda já foi fechada");
    }

    @Test
    void itDoesNotCloseWithItemsStillOnTheWay() {
        OrderItem guioza = item("Guioza", 29_90, 2).markReady(NOW);
        OrderItem cha = item("Chá verde", 12_00, 1);
        OrderItem delivered = item("Sunomono", 19_90, 1).deliver(carlos, NOW);

        assertThatThrownBy(() -> tab.close(carlos, PaymentMethod.CARD, List.of(guioza, cha, delivered), NOW))
                .isInstanceOf(TabStateException.class)
                .hasMessage("Ainda há itens a caminho: 2× Guioza, 1× Chá verde. "
                        + "Entregue ou cancele antes de fechar.");
    }

    @Test
    void anEmptyTabClosesAtZero() {
        assertThat(tab.close(carlos, PaymentMethod.CASH, List.of(), NOW).total()).isEqualTo(Money.ZERO);
    }
}
