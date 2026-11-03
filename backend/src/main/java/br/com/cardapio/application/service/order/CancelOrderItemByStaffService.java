package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.CancelOrderItemByStaffUseCase;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderItemNotFoundException;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class CancelOrderItemByStaffService implements CancelOrderItemByStaffUseCase {

    private final OrderPort orders;
    private final EventPublisherPort events;
    private final Clock clock;

    public CancelOrderItemByStaffService(OrderPort orders, EventPublisherPort events, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void cancel(EstablishmentId establishmentId, OrderItemId itemId, UserId by, String reason) {
        OrderItem item = orders.findItem(establishmentId, itemId).orElseThrow(OrderItemNotFoundException::new);
        Order order = orders.findOrder(establishmentId, item.orderId()).orElseThrow(OrderItemNotFoundException::new);
        Instant now = clock.instant();
        OrderItem canceled = item.cancelByStaff(by, reason, now);
        if (!orders.updateItem(canceled, item.status())) {
            throw new OrderItemStateException("Este item acabou de mudar. Atualize a tela.");
        }
        events.publish(new OrderChanged(establishmentId, order.tableId(), order.id(), order.status(), now));
    }
}
