package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.AdvanceOrderItemUseCase;
import br.com.cardapio.application.port.in.order.OrderItemStep;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderItemNotFoundException;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.order.exception.OrderStateException;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class AdvanceOrderItemService implements AdvanceOrderItemUseCase {

    private final OrderPort orders;
    private final EventPublisherPort events;
    private final Clock clock;

    public AdvanceOrderItemService(OrderPort orders, EventPublisherPort events, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Lê o item, pede ao domínio o próximo passo e grava SÓ SE o item ainda estiver como foi lido:
     * a cozinha marcando "pronto" enquanto o cliente cancela não deixa os dois valerem.
     */
    @Override
    @Transactional
    public void advance(EstablishmentId establishmentId, OrderItemId itemId, OrderItemStep step, UserId by) {
        OrderItem item = orders.findItem(establishmentId, itemId).orElseThrow(OrderItemNotFoundException::new);
        Order order = orders.findOrder(establishmentId, item.orderId()).orElseThrow(OrderItemNotFoundException::new);
        if (order.status() != OrderStatus.SENT) {
            throw new OrderStateException("Este pedido ainda espera a confirmação do garçom");
        }
        Instant now = clock.instant();
        OrderItem changed = switch (step) {
            case START -> item.start(now);
            case READY -> item.markReady(now);
            case DELIVER -> item.deliver(by, now);
        };
        if (!orders.updateItem(changed, item.status())) {
            throw new OrderItemStateException("Este item acabou de mudar. Atualize a tela.");
        }
        events.publish(new OrderChanged(establishmentId, order.tableId(), order.id(), order.status(), now));
    }
}
