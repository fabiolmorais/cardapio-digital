package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.PlaceOrderUseCase;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.PricedSelection;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
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
import br.com.cardapio.domain.rodizio.exception.ProductOnlyInRodizioException;
import br.com.cardapio.domain.rodizio.exception.RodizioNotJoinedException;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioParticipation;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.service.RoundPolicy;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    private final ProductOptionsPort productOptions;
    private final TabPort tabs;
    private final OrderPort orders;
    private final TabRodizioPort tabRodizios;
    private final RodizioPort rodizios;
    private final RodizioPhonePort phones;
    private final SubscriptionPort subscriptions;
    private final EventPublisherPort events;
    private final Clock clock;

    public PlaceOrderService(DiningTablePort tables, TableSessionPort sessions, OrderSettingsPort settings,
                             ProductPort products, ProductOptionsPort productOptions, TabPort tabs, OrderPort orders,
                             TabRodizioPort tabRodizios, RodizioPort rodizios, RodizioPhonePort phones,
                             SubscriptionPort subscriptions, EventPublisherPort events, Clock clock) {
        this.productOptions = productOptions;
        this.phones = phones;
        this.tables = tables;
        this.sessions = sessions;
        this.settings = settings;
        this.products = products;
        this.tabs = tabs;
        this.orders = orders;
        this.tabRodizios = tabRodizios;
        this.rodizios = rodizios;
        this.subscriptions = subscriptions;
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
        // Assinatura passou da tolerância: a mesa não chama nem pede pelo celular (Guia 31).
        PlanAllowances.requireServing(subscriptions, establishmentId, now);

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

        // A rodada só vale com o rodízio ativo na comanda, só com os produtos incluídos nele e dentro
        // das regras que o restaurante ligou. O carrinho não leva o que é só do rodízio.
        TabRodizio rodizio = command.round()
                ? requireRodizio(establishmentId, tableId, visit.session().id(), menu, command.lines(), now)
                : null;
        if (rodizio == null) {
            menu.values().stream().filter(p -> !p.details().aLaCarte()).findFirst().ifPresent(p -> {
                throw new ProductOnlyInRodizioException(p.details().name());
            });
        }
        // O preço de cada linha com o tamanho e as opções (ADR 0010), calculado aqui e conferido contra
        // o cardápio de agora: o que esgotou, saiu ou passou do limite do grupo volta para o cliente.
        Map<ProductId, ProductOptions> configured = productOptions.findFor(establishmentId, menu.keySet());
        List<PricedSelection> priced = command.lines().stream()
                .map(line -> configured.getOrDefault(line.productId(), ProductOptions.none(line.productId()))
                        .price(menu.get(line.productId()).details(), line.selection(), rodizio != null))
                .toList();
        Tab tab = Tabs.openOrCreate(tabs, establishmentId, tableId, now);
        int number = orders.nextNumber(establishmentId, now);
        Money total = Money.ZERO;
        for (int i = 0; i < command.lines().size(); i++) {
            total = total.plus(priced.get(i).unitPrice().times(command.lines().get(i).quantity()));
        }
        int count = command.lines().stream().mapToInt(OrderLine::quantity).sum();

        Order order = Order.place(establishmentId, tab.id(), tableId, visit.session().id(), number,
                rules.needsApproval(total, count), now);
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < command.lines().size(); i++) {
            OrderLine line = command.lines().get(i);
            Product product = menu.get(line.productId());
            items.add(rodizio != null
                    ? OrderItem.inRound(order.id(), product, line, rodizio.id(), priced.get(i).choices(), now)
                    : OrderItem.from(order.id(), product, line, priced.get(i), now));
        }
        orders.insert(order, items);
        events.publish(new OrderChanged(establishmentId, tableId, order.id(), order.status(), now));
        return TableOrder.from(new OrderWithItems(order, items), visit.session().id());
    }

    /**
     * O rodízio da comanda aberta, conferindo que cada produto da rodada é incluído nele e que a
     * rodada respeita as regras. Os produtos e as regras vêm do cadastro de agora: se o restaurante
     * tirou um produto do rodízio, ele sai da rodada também.
     */
    private TabRodizio requireRodizio(EstablishmentId establishmentId, DiningTableId tableId, TableSessionId session,
                                      Map<ProductId, Product> menu, List<OrderLine> lines, Instant now) {
        Tab tab = tabs.findOpen(establishmentId, tableId).orElseThrow(NoRodizioOnTableException::new);
        TabRodizio rodizio = tabRodizios.findByTab(establishmentId, tab.id())
                .orElseThrow(NoRodizioOnTableException::new);
        // O rodízio é cobrado por pessoa: o celular que chegou depois da ativação só pede rodada
        // depois que o garçom o inclui.
        if (phones.find(establishmentId, rodizio.id(), session) != RodizioParticipation.JOINED) {
            throw new RodizioNotJoinedException();
        }
        Rodizio registered = rodizios.find(establishmentId, rodizio.rodizioId()).orElse(null);
        Set<ProductId> included = registered == null ? Set.of() : registered.productIds();
        menu.values().stream().filter(p -> !included.contains(p.id())).findFirst().ifPresent(p -> {
            throw new ProductNotInRodizioException(p.details().name());
        });
        RodizioRules rules = registered == null ? RodizioRules.NONE : registered.rules();
        RodizioRounds.policy(rules, rodizio, orders.findByTab(establishmentId, tab.id())).check(
                lines.stream().map(l -> new RoundPolicy.Line(menu.get(l.productId()).details().name(),
                        l.quantity())).toList(), now);
        return rodizio;
    }
}
