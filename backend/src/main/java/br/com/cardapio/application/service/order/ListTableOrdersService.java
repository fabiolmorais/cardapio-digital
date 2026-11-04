package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ListTableOrdersUseCase;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.in.order.TableOrders;
import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.service.RoundPolicy;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ListTableOrdersService implements ListTableOrdersUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final OrderSettingsPort settings;
    private final TabPort tabs;
    private final OrderPort orders;
    private final TabRodizioPort tabRodizios;
    private final RodizioPort rodizios;
    private final Clock clock;

    public ListTableOrdersService(DiningTablePort tables, TableSessionPort sessions, OrderSettingsPort settings,
                                  TabPort tabs, OrderPort orders, TabRodizioPort tabRodizios, RodizioPort rodizios,
                                  Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.settings = settings;
        this.tabs = tabs;
        this.orders = orders;
        this.tabRodizios = tabRodizios;
        this.rodizios = rodizios;
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
        Optional<Tab> open = tabs.findOpen(establishmentId, visit.table().id());
        List<OrderWithItems> found = open.map(tab -> orders.findByTab(establishmentId, tab.id())).orElse(List.of());
        TabRodizio rodizio = open.flatMap(tab -> tabRodizios.findByTab(establishmentId, tab.id())).orElse(null);
        // Os produtos incluídos e as regras vêm do cadastro de agora (a aba Rodízio mostra o que dá
        // para pedir, e até quanto).
        Rodizio registered = rodizio == null ? null : rodizios.find(establishmentId, rodizio.rodizioId()).orElse(null);
        Set<ProductId> included = registered == null ? Set.of() : registered.productIds();
        RoundPolicy.Limits limits = rodizio == null ? null
                : RodizioRounds.policy(registered == null ? RodizioRules.NONE : registered.rules(), rodizio, found)
                        .limits(now);
        // O total conta o que vai para a conta: nem item cancelado, nem pedido recusado pelo garçom.
        Money total = found.stream()
                .filter(o -> o.order().status() != OrderStatus.REJECTED)
                .flatMap(o -> o.items().stream())
                .filter(i -> i.status() != OrderItemStatus.CANCELED)
                .map(OrderItem::subtotal)
                .reduce(Money.ZERO, Money::plus)
                .plus(rodizio == null ? Money.ZERO : rodizio.total());
        return new TableOrders(enabled, total, rodizio == null ? null : TableRodizio.from(rodizio), included, limits,
                found.stream().map(o -> TableOrder.from(o, visit.session().id())).toList());
    }
}
