package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.exception.OrderStateException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-11-03T20:00:00Z");
    private final UserId carlos = UserId.newId();

    private Order order(boolean needsApproval) {
        return Order.place(EstablishmentId.newId(), TabId.newId(), DiningTableId.newId(), TableSessionId.newId(), 7,
                needsApproval, NOW);
    }

    @Test
    void theWaiterApprovesAWaitingOrder() {
        Order approved = order(true).approve(carlos, NOW.plusSeconds(30));

        assertThat(approved.status()).isEqualTo(OrderStatus.SENT);
        assertThat(approved.approvedBy()).isEqualTo(carlos);
        assertThat(approved.approvedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(approved.number()).isEqualTo(7);
    }

    @Test
    void theWaiterRejectsAWaitingOrder() {
        Order rejected = order(true).reject(carlos, NOW);

        assertThat(rejected.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(rejected.approvedBy()).isEqualTo(carlos);
    }

    @Test
    void onlyAWaitingOrderIsAnswered() {
        assertThatThrownBy(() -> order(false).approve(carlos, NOW))
                .isInstanceOf(OrderStateException.class).hasMessage("Este pedido já foi para a cozinha");
        Order rejected = order(true).reject(carlos, NOW);
        assertThatThrownBy(() -> rejected.approve(carlos, NOW))
                .isInstanceOf(OrderStateException.class).hasMessage("Este pedido já foi recusado");
    }

    private Order byStaff() {
        return Order.placeByStaff(EstablishmentId.newId(), TabId.newId(), DiningTableId.newId(), carlos, 8,
                UUID.randomUUID(), false, NOW);
    }

    @Test
    void theStaffOrderGoesStraightToTheKitchen() {
        Order order = byStaff();

        assertThat(order.status()).isEqualTo(OrderStatus.SENT);
        assertThat(order.byStaff()).isTrue();
        assertThat(order.origin()).isEqualTo(new OrderOrigin.Staff(carlos));
        assertThat(order.placedBy(TableSessionId.newId())).isFalse();
    }

    @Test
    void thePhoneOrderBelongsToWhoSentIt() {
        TableSessionId phone = TableSessionId.newId();
        Order order = Order.place(EstablishmentId.newId(), TabId.newId(), DiningTableId.newId(), phone, 1, false, NOW);

        assertThat(order.byStaff()).isFalse();
        assertThat(order.placedBy(phone)).isTrue();
        assertThat(order.placedBy(TableSessionId.newId())).isFalse();
    }

    @Test
    void aPhoneOrderFromBeforeTheVisitStaysHidden() {
        assertThat(order(false).belongsToVisit(NOW)).isTrue();
        assertThat(order(false).belongsToVisit(NOW.plusSeconds(1))).isFalse();
    }

    @Test
    void aStaffOrderUpToTwoHoursBeforeTheVisitIsPartOfIt() {
        assertThat(byStaff().belongsToVisit(NOW.plus(Duration.ofHours(2)))).isTrue();
        assertThat(byStaff().belongsToVisit(NOW.plus(Duration.ofHours(2)).plusSeconds(1))).isFalse();
    }
}
