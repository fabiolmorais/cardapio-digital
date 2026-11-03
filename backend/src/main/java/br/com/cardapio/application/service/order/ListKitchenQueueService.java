package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.KitchenItem;
import br.com.cardapio.application.port.in.order.ListKitchenQueueUseCase;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

public class ListKitchenQueueService implements ListKitchenQueueUseCase {

    /** O que a fila mostra: o que ainda não chegou à mesa. Entregue e cancelado saem dela. */
    private static final Set<OrderItemStatus> ON_THE_WAY =
            EnumSet.of(OrderItemStatus.PENDING, OrderItemStatus.PREPARING, OrderItemStatus.READY);

    private final OrderPort orders;

    public ListKitchenQueueService(OrderPort orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<KitchenItem> list(EstablishmentId establishmentId, Set<OrderItemStatus> statuses,
                                        PageQuery page) {
        if (!ON_THE_WAY.containsAll(statuses)) {
            throw new InvalidValueException("A fila só tem itens na fila, em preparo ou prontos.");
        }
        Set<OrderItemStatus> wanted = statuses.isEmpty() ? ON_THE_WAY : statuses;
        return orders.findQueue(establishmentId, wanted, page).map(KitchenItem::from);
    }
}
