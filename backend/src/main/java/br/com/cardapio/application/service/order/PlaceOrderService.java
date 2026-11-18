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
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.OrderingDisabledException;
import br.com.cardapio.domain.order.exception.TooManyOrdersException;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.exception.RodizioNotJoinedException;
import br.com.cardapio.domain.rodizio.model.RodizioParticipation;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class PlaceOrderService implements PlaceOrderUseCase {

    /** Linhas diferentes num envio (cada uma com até 20 unidades). */
    public static final int MAX_LINES = OrderDraft.MAX_LINES;
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
        OrderDraft.requireValidSize(command.lines());
        if (orders.countPlacedBySessionAfter(establishmentId, visit.session().id(), now.minus(Duration.ofMinutes(1)))
                >= MAX_PER_MINUTE) {
            throw new TooManyOrdersException();
        }

        OrderDraft draft = OrderDraft.of(products, establishmentId, command.lines());

        // A rodada só vale com o rodízio ativo na comanda, só com os produtos incluídos nele e dentro
        // das regras que o restaurante ligou. O carrinho não leva o que é só do rodízio.
        TabRodizio rodizio = command.round()
                ? requireRodizio(establishmentId, tableId, visit.session().id(), draft, now)
                : null;
        if (rodizio == null) {
            draft.requireALaCarte();
        }
        OrderDraft.Priced priced = draft.priced(productOptions, rodizio != null);
        Tab tab = Tabs.openOrCreate(tabs, establishmentId, tableId, now);
        // Um de cada vez na comanda: o garçom que lança agora enxerga este pedido (ADR 0011).
        Tabs.lock(tabs, tab);
        int number = orders.nextNumber(establishmentId, now);

        Order order = Order.place(establishmentId, tab.id(), tableId, visit.session().id(), number,
                rules.needsApproval(priced.total(), priced.count()), now);
        List<OrderItem> items = priced.items(order.id(), rodizio, now);
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
                                      OrderDraft draft, Instant now) {
        Tab tab = tabs.findOpen(establishmentId, tableId).orElseThrow(NoRodizioOnTableException::new);
        TabRodizio rodizio = tabRodizios.findByTab(establishmentId, tab.id())
                .orElseThrow(NoRodizioOnTableException::new);
        // O rodízio é cobrado por pessoa: o celular que chegou depois da ativação só pede rodada
        // depois que o garçom o inclui.
        if (phones.find(establishmentId, rodizio.id(), session) != RodizioParticipation.JOINED) {
            throw new RodizioNotJoinedException();
        }
        RodizioRounds.requireAllowed(rodizios, orders, tab, rodizio, draft.menu(), draft.lines(), now);
        return rodizio;
    }
}
