package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.PlaceOrderUseCase;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderingDisabledException;
import br.com.cardapio.domain.order.exception.ProductUnavailableException;
import br.com.cardapio.domain.order.exception.TooManyOrdersException;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.exception.ProductNotInRodizioException;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlaceOrderService implements PlaceOrderUseCase {

    /** Linhas diferentes num envio (cada uma com até 20 unidades). */
    public static final int MAX_LINES = 30;
    /** Envios por minuto do mesmo celular. Um carrinho de verdade é um envio; isto segura o trote. */
    public static final int MAX_PER_MINUTE = 3;

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final OrderSettingsPort settings;
    private final ProductPort products;
    private final TabPort tabs;
    private final OrderPort orders;
    private final TabRodizioPort tabRodizios;
    private final RodizioPort rodizios;
    private final EventPublisherPort events;
    private final Clock clock;

    public PlaceOrderService(DiningTablePort tables, TableSessionPort sessions, OrderSettingsPort settings,
                             ProductPort products, TabPort tabs, OrderPort orders, TabRodizioPort tabRodizios,
                             RodizioPort rodizios, EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.settings = settings;
        this.products = products;
        this.tabs = tabs;
        this.orders = orders;
        this.tabRodizios = tabRodizios;
        this.rodizios = rodizios;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TableOrder place(PlaceOrderCommand command) {
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, command.tableToken(), command.sessionToken(), now);
        EstablishmentId establishmentId = visit.table().establishmentId();
        DiningTableId tableId = visit.table().id();

        // As travas de sempre: celular bloqueado, presença no salão, pedido ligado.
        if (visit.session().isBlocked()) {
            throw new SessionBlockedException();
        }
        if (!visit.session().isPresent()) {
            throw new PresenceRequiredException();
        }
        OrderSettings rules = settings.find(establishmentId)
                .orElseGet(() -> OrderSettings.defaults(establishmentId, now));
        if (!rules.orderingEnabled()) {
            throw new OrderingDisabledException();
        }
        if (command.lines().isEmpty() || command.lines().size() > MAX_LINES) {
            throw new InvalidValueException("O pedido deve ter de 1 a " + MAX_LINES + " itens diferentes");
        }
        if (orders.countPlacedBySessionAfter(establishmentId, visit.session().id(), now.minus(Duration.ofMinutes(1)))
                >= MAX_PER_MINUTE) {
            throw new TooManyOrdersException();
        }

        // O cardápio de agora, numa consulta só: o nome e o preço vêm daqui, nunca do celular.
        Map<ProductId, Product> menu = products.findActiveByIds(establishmentId,
                        command.lines().stream().map(OrderLine::productId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        for (OrderLine line : command.lines()) {
            Product product = menu.get(line.productId());
            if (product == null) {
                throw new ProductUnavailableException(null);
            }
            if (!product.available()) {
                throw new ProductUnavailableException(product.details().name());
            }
        }

        // A rodada só vale com o rodízio ativo na comanda, e só com os produtos incluídos nele.
        TabRodizio rodizio = command.round() ? requireRodizio(establishmentId, tableId, menu) : null;
        Tab tab = Tabs.openOrCreate(tabs, establishmentId, tableId, now);
        int number = orders.nextNumber(establishmentId, now);
        Money total = rodizio != null ? Money.ZERO : command.lines().stream()
                .map(line -> menu.get(line.productId()).details().currentPrice().times(line.quantity()))
                .reduce(Money.ZERO, Money::plus);
        int count = command.lines().stream().mapToInt(OrderLine::quantity).sum();

        Order order = Order.place(establishmentId, tab.id(), tableId, visit.session().id(), number,
                rules.needsApproval(total, count), now);
        List<OrderItem> items = command.lines().stream()
                .map(line -> rodizio != null
                        ? OrderItem.inRound(order.id(), menu.get(line.productId()), line, rodizio.id(), now)
                        : OrderItem.from(order.id(), menu.get(line.productId()), line, now))
                .toList();
        orders.insert(order, items);
        events.publish(new OrderChanged(establishmentId, tableId, order.id(), order.status(), now));
        return TableOrder.from(new OrderWithItems(order, items), visit.session().id());
    }

    /**
     * O rodízio da comanda aberta, conferindo que cada produto da rodada é incluído nele. Os
     * produtos vêm do cadastro de agora: se o restaurante tirou um produto do rodízio, ele sai da
     * rodada também.
     */
    private TabRodizio requireRodizio(EstablishmentId establishmentId, DiningTableId tableId,
                                      Map<ProductId, Product> menu) {
        TabRodizio rodizio = tabs.findOpen(establishmentId, tableId)
                .flatMap(tab -> tabRodizios.findByTab(establishmentId, tab.id()))
                .orElseThrow(NoRodizioOnTableException::new);
        Set<ProductId> included = rodizios.find(establishmentId, rodizio.rodizioId())
                .map(r -> r.productIds()).orElse(Set.of());
        menu.values().stream().filter(p -> !included.contains(p.id())).findFirst().ifPresent(p -> {
            throw new ProductNotInRodizioException(p.details().name());
        });
        return rodizio;
    }
}
