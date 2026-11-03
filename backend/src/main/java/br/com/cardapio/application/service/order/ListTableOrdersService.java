package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ListTableOrdersUseCase;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.in.order.TableOrders;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class ListTableOrdersService implements ListTableOrdersUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final OrderSettingsPort settings;
    private final TabPort tabs;
    private final OrderPort orders;
    private final Clock clock;

    public ListTableOrdersService(DiningTablePort tables, TableSessionPort sessions, OrderSettingsPort settings,
                                  TabPort tabs, OrderPort orders, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.settings = settings;
        this.tabs = tabs;
        this.orders = orders;
        this.clock = clock;
    }

    /** Não é readOnly: olhar os pedidos conta como uso, e renova a sessão (como os chamados). */
    @Override
    @Transactional
    public TableOrders list(String tableToken, String sessionToken) {
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, now);
        var establishmentId = visit.table().establishmentId();
        boolean enabled = settings.find(establishmentId).orElseGet(() -> OrderSettings.defaults(establishmentId, now))
                .orderingEnabled();
        List<OrderWithItems> found = tabs.findOpen(establishmentId, visit.table().id())
                .map(tab -> orders.findByTab(establishmentId, tab.id()))
                .orElse(List.of());
        // O total conta o que vai para a conta: nem item cancelado, nem pedido recusado pelo garçom.
        Money total = found.stream()
                .filter(o -> o.order().status() != OrderStatus.REJECTED)
                .flatMap(o -> o.items().stream())
                .filter(i -> i.status() != OrderItemStatus.CANCELED)
                .map(OrderItem::subtotal)
                .reduce(Money.ZERO, Money::plus);
        return new TableOrders(enabled, total,
                found.stream().map(o -> TableOrder.from(o, visit.session().id())).toList());
    }
}
