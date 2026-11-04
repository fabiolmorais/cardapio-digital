package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabStatus;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.List;

/**
 * A comanda como a EQUIPE vê: todos os pedidos, de todos os celulares, com o passo de cada item,
 * o total e, se a mesa pediu a conta, a forma de pagamento que escolheu.
 *
 * @param total             aberta: a soma de agora; fechada: o que foi cobrado
 * @param paymentMethod     fechada: como pagou; null enquanto aberta
 * @param billRequestedWith aberta e com a conta pedida: a forma escolhida pela mesa; senão null
 * @param rodizio           o rodízio da comanda (já somado no total); null se não houve
 */
public record StaffTab(TabId id, DiningTableId tableId, String tableLabel, String sectorName, TabStatus status,
                       Instant openedAt,
                       Instant closedAt, Money total, PaymentMethod paymentMethod, PaymentMethod billRequestedWith,
                       TableRodizio rodizio, List<Order> orders) {

    public StaffTab {
        orders = List.copyOf(orders);
    }

    public record Order(OrderId id, int number, OrderStatus status, Instant createdAt, List<TableOrder.Item> items) {

        public Order {
            items = List.copyOf(items);
        }
    }

    /** @param rodizio o rodízio da comanda; null se não houve */
    public static StaffTab from(TabView view, List<OrderWithItems> found, PaymentMethod billRequestedWith,
                                TabRodizio rodizio) {
        Tab tab = view.tab();
        List<OrderItem> all = found.stream().flatMap(o -> o.items().stream()).toList();
        Money total = tab.total() != null ? tab.total()
                : Tab.totalOf(all).plus(rodizio == null ? Money.ZERO : rodizio.total());
        return new StaffTab(tab.id(), tab.tableId(), view.tableLabel(), view.sectorName(), tab.status(), tab.openedAt(),
                tab.closedAt(), total, tab.paymentMethod(), billRequestedWith,
                rodizio == null ? null : TableRodizio.from(rodizio),
                found.stream().map(o -> new Order(o.order().id(), o.order().number(), o.order().status(),
                        o.order().createdAt(), o.items().stream().map(TableOrder.Item::from).toList())).toList());
    }
}
