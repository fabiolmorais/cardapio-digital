package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.AlreadyOrdered;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderOrigin;
import br.com.cardapio.domain.order.service.DuplicateOrderPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** O que a comanda tem de "já pedido", com o nome de quem lançou (ADR 0011). */
final class AlreadyOrderedItems {

    private AlreadyOrderedItems() {
    }

    static List<AlreadyOrdered> of(OrderPort orders, EstablishmentId establishmentId, List<OrderWithItems> found,
                                   Instant now) {
        Map<UserId, String> names = orders.findStaffNames(establishmentId, StaffTab.staffOf(found));
        List<AlreadyOrdered> result = new ArrayList<>();
        for (OrderWithItems placed : found) {
            Order order = placed.order();
            String name = order.origin() instanceof OrderOrigin.Staff staff ? names.get(staff.userId()) : null;
            for (OrderItem item : placed.items()) {
                if (DuplicateOrderPolicy.counts(order, item, now)) {
                    result.add(new AlreadyOrdered(item.productId(), item.productName(), item.quantity(),
                            item.choices(), item.status(), order.createdAt(), order.byStaff(), name));
                }
            }
        }
        return result;
    }
}
