package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ReviewOrderUseCase;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderNotFoundException;
import br.com.cardapio.domain.order.exception.OrderStateException;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class ReviewOrderService implements ReviewOrderUseCase {

    private final OrderPort orders;
    private final EventPublisherPort events;
    private final Clock clock;

    public ReviewOrderService(OrderPort orders, EventPublisherPort events, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    /** Confirmado: os itens entram na fila da cozinha, na ordem de chegada do pedido. */
    @Override
    @Transactional
    public void approve(EstablishmentId establishmentId, OrderId orderId, UserId by) {
        Order order = orders.findOrder(establishmentId, orderId).orElseThrow(OrderNotFoundException::new);
        Instant now = clock.instant();
        save(order.approve(by, now), now);
    }

    /** Recusado: o pedido e os itens saem de cena, e o cliente vê o motivo nos itens. */
    @Override
    @Transactional
    public void reject(EstablishmentId establishmentId, OrderId orderId, UserId by) {
        Order order = orders.findOrder(establishmentId, orderId).orElseThrow(OrderNotFoundException::new);
        Instant now = clock.instant();
        Order rejected = order.reject(by, now);
        save(rejected, now);
        orders.cancelItemsOnTheWay(establishmentId, orderId, by, OrderItem.REJECTED_BY_STAFF, now);
    }

    /** Dois garçons respondendo ao mesmo pedido: o UPDATE condicional deixa valer só o primeiro. */
    private void save(Order changed, Instant now) {
        if (!orders.updateOrder(changed, OrderStatus.PENDING_APPROVAL)) {
            throw new OrderStateException("Outra pessoa acabou de responder este pedido");
        }
        events.publish(new OrderChanged(changed.establishmentId(), changed.tableId(), changed.id(), changed.status(),
                now));
    }
}
