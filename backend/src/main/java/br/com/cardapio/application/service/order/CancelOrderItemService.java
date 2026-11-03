package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.CancelOrderItemUseCase;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderItemNotFoundException;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class CancelOrderItemService implements CancelOrderItemUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final OrderPort orders;
    private final EventPublisherPort events;
    private final Clock clock;

    public CancelOrderItemService(DiningTablePort tables, TableSessionPort sessions, OrderPort orders,
                                  EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void cancel(String tableToken, String sessionToken, OrderItemId itemId) {
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, now);
        var establishmentId = visit.table().establishmentId();
        OrderItem item = orders.findItem(establishmentId, itemId).orElseThrow(OrderItemNotFoundException::new);
        Order order = orders.findOrder(establishmentId, item.orderId()).orElseThrow(OrderItemNotFoundException::new);
        // O item de outra mesa nem existe para esta (404); o de outro celular da mesa, existe, mas não é dele.
        if (!order.tableId().equals(visit.table().id())) {
            throw new OrderItemNotFoundException();
        }
        if (!order.sessionId().equals(visit.session().id())) {
            throw new OrderItemStateException("Só quem pediu pode cancelar este item.");
        }
        OrderItem canceled = item.cancelByCustomer(now);
        if (!orders.updateItem(canceled, OrderItemStatus.PENDING)) {
            throw new OrderItemStateException(
                    "A cozinha acabou de começar este item. Para cancelar, fale com o garçom.");
        }
        events.publish(new OrderChanged(establishmentId, order.tableId(), order.id(), order.status(), now));
    }
}
