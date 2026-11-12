package br.com.cardapio.adapter.out.persistence.floor;

import br.com.cardapio.application.port.out.live.LiveFloorPort;
import br.com.cardapio.application.port.out.live.LiveTableView;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

/**
 * O movimento do salão agora, em consultas nativas. O filtro do Hibernate não vale em SQL nativo:
 * o restaurante vai no WHERE de cada uma.
 */
@Component
class LiveFloorPersistenceAdapter implements LiveFloorPort {

    private final DiningTableJpaRepository tables;

    LiveFloorPersistenceAdapter(DiningTableJpaRepository tables) {
        this.tables = tables;
    }

    @Override
    public List<LiveTableView> findTables(EstablishmentId establishmentId, Instant now) {
        return tables.findLive(establishmentId.value(), now).stream()
                .map(r -> new LiveTableView(new DiningTableId(r.getTableId()), r.getLabel(),
                        new SectorId(r.getSectorId()), r.getSectorName(), (int) r.getPhones(),
                        r.getTabId() == null ? null : new TabId(r.getTabId()), r.getTabOpenedAt(),
                        Money.ofCents(r.getTabTotalCents()), (int) r.getItemsOnTheWay(), (int) r.getRodizioPeople()))
                .toList();
    }

    @Override
    public Money revenueOn(EstablishmentId establishmentId, Instant now) {
        return Money.ofCents(tables.revenueOn(establishmentId.value(), now));
    }

    @Override
    public OptionalLong averageWaitSeconds(EstablishmentId establishmentId, Instant now) {
        Long seconds = tables.averageWaitSeconds(establishmentId.value(), now);
        return seconds == null ? OptionalLong.empty() : OptionalLong.of(seconds);
    }
}
