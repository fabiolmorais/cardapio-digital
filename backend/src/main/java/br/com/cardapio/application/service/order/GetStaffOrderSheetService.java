package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.GetStaffOrderSheetUseCase;
import br.com.cardapio.application.port.in.order.StaffOrderSheet;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.AlreadyOrdered;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.Tab;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class GetStaffOrderSheetService implements GetStaffOrderSheetUseCase {

    private final DiningTablePort tables;
    private final OrderSettingsPort settings;
    private final TabPort tabs;
    private final OrderPort orders;
    private final TabRodizioPort tabRodizios;
    private final RodizioPort rodizios;
    private final Clock clock;

    public GetStaffOrderSheetService(DiningTablePort tables, OrderSettingsPort settings, TabPort tabs,
                                     OrderPort orders, TabRodizioPort tabRodizios, RodizioPort rodizios, Clock clock) {
        this.tables = tables;
        this.settings = settings;
        this.tabs = tabs;
        this.orders = orders;
        this.tabRodizios = tabRodizios;
        this.rodizios = rodizios;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffOrderSheet get(EstablishmentId establishmentId, DiningTableId tableId) {
        Instant now = clock.instant();
        // O balcão (ADR 0015) lança pela comanda de cada cliente, não pelo lugar.
        DiningTable table = tables.find(establishmentId, tableId).filter(DiningTable::active)
                .filter(found -> !found.isCounter())
                .orElseThrow(TableNotFoundException::new);
        boolean enabled = settings.find(establishmentId)
                .orElseGet(() -> OrderSettings.defaults(establishmentId, now)).staffOrderingEnabled();
        Optional<Tab> open = tabs.findOpen(establishmentId, tableId);
        List<AlreadyOrdered> alreadyOrdered = open
                .map(tab -> AlreadyOrderedItems.of(orders, establishmentId,
                        orders.findByTab(establishmentId, tab.id()), now))
                .orElse(List.of());
        StaffOrderSheet.Rodizio rodizio = open.flatMap(tab -> tabRodizios.findByTab(establishmentId, tab.id()))
                .flatMap(active -> rodizios.find(establishmentId, active.rodizioId()))
                .map(r -> new StaffOrderSheet.Rodizio(r.name(), r.productIds()))
                .orElse(null);
        return new StaffOrderSheet(table.id(), table.label(), enabled, alreadyOrdered, rodizio);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffOrderSheet getForCounterTab(EstablishmentId establishmentId, TabId tabId) {
        Instant now = clock.instant();
        TabView view = tabs.findView(establishmentId, tabId)
                .filter(found -> found.tab().atCounter())
                .orElseThrow(TabNotFoundException::new);
        if (!view.tab().status().isOpen()) {
            throw new TabStateException("Esta comanda já foi fechada");
        }
        List<AlreadyOrdered> alreadyOrdered = AlreadyOrderedItems.of(orders, establishmentId,
                orders.findByTab(establishmentId, tabId), now);
        return new StaffOrderSheet(view.tab().tableId(), view.tableLabel(), true, alreadyOrdered, null,
                view.tab().guest());
    }
}
