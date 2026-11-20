package br.com.cardapio.support;

import br.com.cardapio.application.port.out.order.TabMovePort;
import br.com.cardapio.application.port.out.order.TabMoveView;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabMove;
import br.com.cardapio.domain.order.model.TabMoveId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** As mudanças de mesa dos testes sem banco: guarda o que foi gravado (as mesas saem pelo id). */
public final class InMemoryTabMoves implements TabMovePort {

    public final List<TabMove> saved = new ArrayList<>();

    @Override
    public void save(TabMove move) {
        saved.add(move);
    }

    @Override
    public boolean recordPrintedOnTheWay(EstablishmentId establishmentId, TabMoveId moveId, TabId fromTab) {
        return false; // a impressão é conferida no teste de integração
    }

    @Override
    public List<TabMoveView> findByTab(EstablishmentId establishmentId, TabId tabId) {
        return saved.stream()
                .filter(m -> m.establishmentId().equals(establishmentId) && m.tabId().equals(tabId))
                .map(m -> new TabMoveView(m.fromTableId().value().toString(), m.toTableId().value().toString(),
                        m.merged(), null, m.movedAt()))
                .toList();
    }

    @Override
    public Optional<TabId> findMergedInto(EstablishmentId establishmentId, TabId tabId) {
        return saved.stream()
                .filter(m -> m.establishmentId().equals(establishmentId) && tabId.equals(m.mergedTabId()))
                .map(TabMove::tabId)
                .findFirst();
    }

    @Override
    public boolean movedBetween(EstablishmentId establishmentId, DiningTableId from, DiningTableId to,
                                Instant since) {
        return saved.stream().anyMatch(m -> m.establishmentId().equals(establishmentId)
                && m.fromTableId().equals(from) && m.toTableId().equals(to) && !m.movedAt().isBefore(since));
    }
}
