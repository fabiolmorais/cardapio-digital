package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.order.TabMovePort;
import br.com.cardapio.application.port.out.order.TabMoveView;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabMove;
import br.com.cardapio.domain.order.model.TabMoveId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class TabMovePersistenceAdapter implements TabMovePort {

    private final TabMoveJpaRepository moves;
    private final EstablishmentFilter establishmentFilter;

    TabMovePersistenceAdapter(TabMoveJpaRepository moves, EstablishmentFilter establishmentFilter) {
        this.moves = moves;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void save(TabMove m) {
        establishmentFilter.enable(m.establishmentId());
        moves.saveAndFlush(new TabMoveJpaEntity(m.id().value(), m.establishmentId().value(), m.tabId().value(),
                m.fromTableId().value(), m.toTableId().value(),
                m.mergedTabId() == null ? null : m.mergedTabId().value(), m.movedBy().value(), m.movedAt()));
    }

    @Override
    public boolean recordPrintedOnTheWay(EstablishmentId establishmentId, TabMoveId moveId, TabId fromTab) {
        if (moves.recordPrintedOnTheWay(establishmentId.value(), moveId.value(), fromTab.value()) == 0) {
            return false;
        }
        moves.markNeedsPrint(establishmentId.value(), moveId.value());
        return true;
    }

    @Override
    public List<TabMoveView> findByTab(EstablishmentId establishmentId, TabId tabId) {
        return moves.findByTab(establishmentId.value(), tabId.value()).stream()
                .map(r -> new TabMoveView(r.getFromTableLabel(), r.getToTableLabel(), r.getMerged(),
                        r.getMovedByName(), r.getMovedAt()))
                .toList();
    }

    @Override
    public Optional<TabId> findMergedInto(EstablishmentId establishmentId, TabId tabId) {
        return moves.findMergedInto(establishmentId.value(), tabId.value()).map(TabId::new);
    }

    @Override
    public boolean movedBetween(EstablishmentId establishmentId, DiningTableId from, DiningTableId to,
                                Instant since) {
        return moves.movedBetween(establishmentId.value(), from.value(), to.value(), since);
    }
}
