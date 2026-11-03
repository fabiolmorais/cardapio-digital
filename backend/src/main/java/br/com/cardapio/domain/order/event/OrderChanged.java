package br.com.cardapio.domain.order.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.shared.event.TableEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Um pedido da mesa mudou: chegou, ou um item dele mudou. Como o CallChanged, leva só o mínimo:
 * a equipe e a mesa buscam os itens pela API.
 */
public record OrderChanged(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        OrderId orderId,
        OrderStatus status,
        Instant occurredAt
) implements TableEvent {

    public OrderChanged {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(occurredAt);
    }
}
