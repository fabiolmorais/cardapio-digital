package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.PlaceStaffOrderCommand;
import br.com.cardapio.application.port.in.order.PlaceStaffOrderUseCase;
import br.com.cardapio.application.port.in.order.StaffOrder;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.CounterNotAllowedException;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.event.OrderChanged;
import br.com.cardapio.domain.order.exception.DuplicateOrderException;
import br.com.cardapio.application.port.out.order.DuplicateWarningPort;
import br.com.cardapio.domain.order.exception.StaffOrderingDisabledException;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.AlreadyOrdered;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * O garçom lança o pedido na comanda (ADR 0011). Vai direto para a cozinha: a confirmação, os limites
 * e a trava de presença protegem o pedido do cliente anônimo, e aqui quem pede é a equipe.
 */
public class PlaceStaffOrderService implements PlaceStaffOrderUseCase {

    private final DiningTablePort tables;
    private final OrderSettingsPort settings;
    private final ProductPort products;
    private final ProductOptionsPort productOptions;
    private final TabPort tabs;
    private final OrderPort orders;
    private final TabRodizioPort tabRodizios;
    private final RodizioPort rodizios;
    private final SubscriptionPort subscriptions;
    private final EventPublisherPort events;
    private final DuplicateWarningPort warnings;
    private final Clock clock;

    public PlaceStaffOrderService(DiningTablePort tables, OrderSettingsPort settings, ProductPort products,
                                  ProductOptionsPort productOptions, TabPort tabs, OrderPort orders,
                                  TabRodizioPort tabRodizios, RodizioPort rodizios, SubscriptionPort subscriptions,
                                  EventPublisherPort events, DuplicateWarningPort warnings, Clock clock) {
        this.tables = tables;
        this.settings = settings;
        this.products = products;
        this.productOptions = productOptions;
        this.tabs = tabs;
        this.orders = orders;
        this.tabRodizios = tabRodizios;
        this.rodizios = rodizios;
        this.subscriptions = subscriptions;
        this.events = events;
        this.warnings = warnings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StaffOrder place(PlaceStaffOrderCommand command) {
        Instant now = clock.instant();
        EstablishmentId establishmentId = command.establishmentId();
        // No balcão (ADR 0015), o pedido vai para a comanda do cliente; na mesa, para a comanda da mesa.
        Tab counterTab = command.tabId() == null ? null : counterTab(establishmentId, command.tabId());
        DiningTable table = tables.find(establishmentId, counterTab == null ? command.tableId() : counterTab.tableId())
                .filter(DiningTable::active)
                .orElseThrow(TableNotFoundException::new);
        DiningTableId tableId = table.id();
        if (counterTab == null && table.isCounter()) {
            throw new CounterNotAllowedException("No balcão, escolha a comanda do cliente");
        }
        PlanAllowances.requireServing(subscriptions, establishmentId, now);
        // No balcão, a equipe sempre lança: sem isso, o balcão não funciona. Na mesa, vale a opção.
        boolean enabled = counterTab != null || settings.find(establishmentId)
                .orElseGet(() -> OrderSettings.defaults(establishmentId, now)).staffOrderingEnabled();
        if (!enabled) {
            throw new StaffOrderingDisabledException();
        }
        OrderDraft.requireValidSize(command.lines());

        // A rodada precisa do rodízio já ativo; o pedido comum abre a comanda da mesa se for o primeiro.
        Tab tab = counterTab != null ? counterTab
                : command.round()
                ? tabs.findOpen(establishmentId, tableId).orElseThrow(NoRodizioOnTableException::new)
                : Tabs.openOrCreate(tabs, establishmentId, tableId, now);
        // Um envio de cada vez na comanda: o segundo garçom enxerga o pedido do primeiro.
        Tabs.lock(tabs, tab);

        // O mesmo envio de novo (a resposta se perdeu na rede): devolve o pedido que já existe.
        Optional<Order> sent = orders.findByClientRequest(establishmentId, command.clientRequestId());
        if (sent.isPresent()) {
            if (!sent.get().tableId().equals(tableId) || !sent.get().tabId().equals(tab.id())) {
                throw new InvalidValueException("Este código de envio já foi usado em outra mesa");
            }
            return StaffOrder.from(sent.get());
        }

        OrderDraft draft = OrderDraft.of(products, establishmentId, command.lines());
        TabRodizio rodizio = null;
        boolean duplicateConfirmed = false;
        if (command.round()) {
            rodizio = tabRodizios.findByTab(establishmentId, tab.id()).orElseThrow(NoRodizioOnTableException::new);
            RodizioRounds.requireAllowed(rodizios, orders, tab, rodizio, draft.menu(), draft.lines(), now);
        } else {
            draft.requireALaCarte();
            duplicateConfirmed = checkDuplicates(command, draft, tab, now);
        }
        OrderDraft.Priced priced = draft.priced(productOptions, rodizio != null);

        Order order = Order.placeByStaff(establishmentId, tab.id(), tableId, command.placedBy(),
                orders.nextNumber(establishmentId, now), command.clientRequestId(), duplicateConfirmed, now);
        orders.insert(order, priced.items(order.id(), rodizio, now));
        events.publish(new OrderChanged(establishmentId, tableId, order.id(), order.status(), now));
        return StaffOrder.from(order);
    }

    /** A comanda aberta do balcão: outra comanda (ou de outro restaurante) é 404; fechada, 422. */
    private Tab counterTab(EstablishmentId establishmentId, TabId tabId) {
        Tab tab = tabs.findView(establishmentId, tabId).map(TabView::tab)
                .filter(Tab::atCounter)
                .orElseThrow(TabNotFoundException::new);
        if (!tab.status().isOpen()) {
            throw new TabStateException("Esta comanda já foi fechada");
        }
        return tab;
    }

    /** A trava: para o envio se há repetição sem confirmar. true: havia repetição, e toda ela foi confirmada. */
    private boolean checkDuplicates(PlaceStaffOrderCommand command, OrderDraft draft, Tab tab, Instant now) {
        EstablishmentId establishmentId = command.establishmentId();
        List<AlreadyOrdered> repeated = AlreadyOrderedItems
                .of(orders, establishmentId, orders.findByTab(establishmentId, tab.id()), now).stream()
                .filter(item -> draft.productIds().contains(item.productId()))
                .toList();
        List<AlreadyOrdered> unconfirmed = repeated.stream()
                .filter(item -> !command.confirmedDuplicates().contains(item.productId()))
                .toList();
        if (!unconfirmed.isEmpty()) {
            // O aviso fica gravado (numa transação própria), mesmo com este envio recusado (REL-50).
            warnings.warn(establishmentId, tab.id(), command.placedBy(),
                    unconfirmed.stream().map(AlreadyOrdered::productId).distinct().toList(), now);
            throw new DuplicateOrderException(unconfirmed);
        }
        if (!repeated.isEmpty()) {
            warnings.confirm(establishmentId, tab.id(), command.placedBy(),
                    repeated.stream().map(AlreadyOrdered::productId).distinct().toList(), now);
        }
        return !repeated.isEmpty();
    }
}
