package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ManageCounterTabsUseCase;
import br.com.cardapio.application.port.in.order.OpenCounterTabCommand;
import br.com.cardapio.application.port.in.order.OpenTab;
import br.com.cardapio.application.port.in.order.RenameCounterTabCommand;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.order.OpenTabView;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.order.event.CounterTabChanged;
import br.com.cardapio.domain.order.exception.CounterCardInUseException;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.CounterGuest;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * As comandas do balcão (RF45, ADR 0015). Dono, gerente e garçom abrem a comanda na hora, com o
 * nome do cliente; o controller confere o papel. A assinatura precisa estar valendo, como no pedido.
 */
public class ManageCounterTabsService implements ManageCounterTabsUseCase {

    private final DiningTablePort tables;
    private final SectorPort sectors;
    private final TabPort tabs;
    private final SubscriptionPort subscriptions;
    private final EventPublisherPort events;
    private final Clock clock;

    public ManageCounterTabsService(DiningTablePort tables, SectorPort sectors, TabPort tabs,
                                    SubscriptionPort subscriptions, EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.sectors = sectors;
        this.tabs = tabs;
        this.subscriptions = subscriptions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OpenTab open(OpenCounterTabCommand command) {
        Instant now = clock.instant();
        EstablishmentId establishmentId = command.establishmentId();
        DiningTable counter = counter(establishmentId, command.counterId());
        PlanAllowances.requireServing(subscriptions, establishmentId, now);
        CounterGuest guest = new CounterGuest(command.name(), command.card());
        Tab tab = Tab.openAtCounter(establishmentId, counter.id(), guest, now);
        if (!tabs.insertAtCounter(tab)) {
            throw new CounterCardInUseException(guest.card());
        }
        events.publish(new CounterTabChanged(establishmentId, counter.id(), tab.id(), now));
        String sector = sectors.find(establishmentId, counter.sectorId()).map(Sector::name).orElse("");
        return OpenTab.from(new OpenTabView(tab, counter.label(), sector, Money.ZERO, 0));
    }

    @Override
    @Transactional
    public OpenTab rename(RenameCounterTabCommand command) {
        EstablishmentId establishmentId = command.establishmentId();
        TabView view = tabs.findView(establishmentId, command.tabId()).orElseThrow(TabNotFoundException::new);
        if (!view.tab().atCounter()) {
            throw new TabStateException("Só a comanda do balcão tem o nome do cliente");
        }
        CounterGuest guest = new CounterGuest(command.name(), command.card());
        Tab renamed = view.tab().rename(guest);
        if (!tabs.saveGuest(renamed)) {
            throw new CounterCardInUseException(guest.card());
        }
        events.publish(new CounterTabChanged(establishmentId, renamed.tableId(), renamed.id(), clock.instant()));
        return tabs.findOpenAtCounter(establishmentId, renamed.tableId(), new PageQuery(0, PageQuery.MAX_SIZE))
                .content().stream()
                .filter(open -> open.tab().id().equals(renamed.id()))
                .findFirst()
                .map(OpenTab::from)
                .orElseThrow(() -> new TabStateException("Esta comanda já foi fechada"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<OpenTab> list(EstablishmentId establishmentId, DiningTableId counterId, PageQuery page) {
        counter(establishmentId, counterId);
        return tabs.findOpenAtCounter(establishmentId, counterId, page).map(OpenTab::from);
    }

    /** O balcão ativo deste restaurante: outro lugar (ou de outro restaurante) é 404. */
    private DiningTable counter(EstablishmentId establishmentId, DiningTableId id) {
        return tables.find(establishmentId, id)
                .filter(DiningTable::active)
                .filter(DiningTable::isCounter)
                .orElseThrow(TableNotFoundException::new);
    }
}
