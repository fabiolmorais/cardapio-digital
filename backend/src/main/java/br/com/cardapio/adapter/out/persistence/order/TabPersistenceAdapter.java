package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.order.ClosedTabView;
import br.com.cardapio.application.port.out.order.OpenTabView;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeriod;
import br.com.cardapio.domain.order.model.TabStatus;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class TabPersistenceAdapter implements TabPort {

    private final TabJpaRepository tabs;
    private final EstablishmentFilter establishmentFilter;

    TabPersistenceAdapter(TabJpaRepository tabs, EstablishmentFilter establishmentFilter) {
        this.tabs = tabs;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public Optional<Tab> findOpen(EstablishmentId establishmentId, DiningTableId tableId) {
        establishmentFilter.enable(establishmentId);
        return tabs.findOpen(establishmentId.value(), tableId.value()).map(TabPersistenceAdapter::toDomain);
    }

    @Override
    public boolean insertIfNoneOpen(Tab tab) {
        return tabs.insertIfNoneOpen(tab.id().value(), tab.establishmentId().value(), tab.tableId().value(),
                tab.openedAt()) == 1;
    }

    @Override
    public Optional<TabView> findView(EstablishmentId establishmentId, TabId tabId) {
        establishmentFilter.enable(establishmentId);
        return tabs.findView(tabId.value(), establishmentId.value())
                .map(r -> new TabView(toDomain(r.tab()), r.tableLabel(), r.sectorName()));
    }

    @Override
    public boolean close(Tab t) {
        establishmentFilter.enable(t.establishmentId());
        return tabs.close(t.id().value(), t.establishmentId().value(), t.closedAt(), t.closedBy().value(),
                t.total().cents(), t.paymentMethod() == null ? null : t.paymentMethod().name()) == 1;
    }

    @Override
    public ResultPage<OpenTabView> findOpenTabs(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<OpenTabRow> rows = tabs.findOpenTabs(establishmentId.value(), PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(r -> new OpenTabView(toDomain(r.tab()), r.tableLabel(), r.sectorName(),
                Money.ofCents(r.totalCents()), r.itemsOnTheWay().intValue())).getContent(), page.page(),
                page.size(), rows.getTotalElements());
    }

    @Override
    public ResultPage<ClosedTabView> findClosed(EstablishmentId establishmentId, TabPeriod period,
                                                DiningTableId tableId, PageQuery page) {
        Page<ClosedTabRow> rows = tabs.findClosed(establishmentId.value(), period.from(), period.to(),
                tableId == null ? null : tableId.value(), PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(r -> new ClosedTabView(new TabId(r.getId()), r.getTableLabel(),
                r.getSectorName(), r.getOpenedAt(), r.getClosedAt(), Money.ofCents(r.getTotalCents()),
                r.getPaymentMethod() == null ? null : PaymentMethod.valueOf(r.getPaymentMethod()),
                r.getClosedByName())).getContent(), page.page(),
                page.size(), rows.getTotalElements());
    }

    private static Tab toDomain(TabJpaEntity e) {
        return new Tab(new TabId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new DiningTableId(e.getTableId()), TabStatus.valueOf(e.getStatus()), e.getOpenedAt(),
                e.getClosedAt(), e.getClosedBy() == null ? null : new UserId(e.getClosedBy()),
                e.getTotalCents() == null ? null : Money.ofCents(e.getTotalCents()),
                e.getPaymentMethod() == null ? null : PaymentMethod.valueOf(e.getPaymentMethod()));
    }
}
