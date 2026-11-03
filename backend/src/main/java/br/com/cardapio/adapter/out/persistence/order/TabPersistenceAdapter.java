package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabStatus;
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
        return tabs.findOpen(establishmentId.value(), tableId.value()).map(e -> new Tab(new TabId(e.getId()),
                new EstablishmentId(e.getEstablishmentId()), new DiningTableId(e.getTableId()),
                TabStatus.valueOf(e.getStatus()), e.getOpenedAt(), e.getClosedAt(),
                e.getClosedBy() == null ? null : new UserId(e.getClosedBy())));
    }

    @Override
    public boolean insertIfNoneOpen(Tab tab) {
        return tabs.insertIfNoneOpen(tab.id().value(), tab.establishmentId().value(), tab.tableId().value(),
                tab.openedAt()) == 1;
    }
}
