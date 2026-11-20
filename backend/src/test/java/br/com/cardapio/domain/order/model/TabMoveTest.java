package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.exception.TabMoveException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabMoveTest {

    private static final Instant NOW = Instant.parse("2026-11-19T23:00:00Z");

    private final EstablishmentId sakura = EstablishmentId.newId();
    private final UserId joao = UserId.newId();
    private final DiningTableId four = DiningTableId.newId();
    private final DiningTableId twelve = DiningTableId.newId();

    private Tab openAt(DiningTableId table, Instant openedAt) {
        return Tab.open(sakura, table, openedAt);
    }

    @Test
    void toAFreeTableTheSameTabGoesToTheNewTable() {
        Tab tab = openAt(four, NOW.minusSeconds(3600));

        TabMove move = TabMove.toFreeTable(tab, twelve, joao, NOW);
        Tab moved = move.applyTo(tab, null);

        assertThat(move.merged()).isFalse();
        assertThat(move.tabId()).isEqualTo(tab.id());
        assertThat(move.fromTableId()).isEqualTo(four);
        assertThat(moved.id()).isEqualTo(tab.id());
        assertThat(moved.tableId()).isEqualTo(twelve);
        assertThat(moved.openedAt()).isEqualTo(tab.openedAt());
    }

    @Test
    void mergingKeepsTheDestinationTabOpenSinceTheOlderOfTheTwo() {
        Tab source = openAt(four, NOW.minusSeconds(7200));
        Tab destination = openAt(twelve, NOW.minusSeconds(600));

        TabMove move = TabMove.merge(source, destination, joao, NOW);
        Tab kept = move.applyTo(destination, source);

        assertThat(move.merged()).isTrue();
        assertThat(move.tabId()).isEqualTo(destination.id());
        assertThat(move.mergedTabId()).isEqualTo(source.id());
        assertThat(kept.id()).isEqualTo(destination.id());
        assertThat(kept.tableId()).isEqualTo(twelve);
        assertThat(kept.openedAt()).isEqualTo(source.openedAt());
    }

    @Test
    void theSameTableOrAClosedTabCannotMove() {
        Tab tab = openAt(four, NOW);
        Tab closed = tab.close(joao, PaymentMethod.PIX, List.of(), NOW);

        assertThatThrownBy(() -> TabMove.toFreeTable(tab, four, joao, NOW)).isInstanceOf(TabMoveException.class);
        assertThatThrownBy(() -> TabMove.merge(tab, tab, joao, NOW)).isInstanceOf(TabMoveException.class);
        assertThatThrownBy(() -> TabMove.toFreeTable(closed, twelve, joao, NOW))
                .isInstanceOf(TabStateException.class);
        assertThatThrownBy(() -> TabMove.merge(tab, closed, joao, NOW)).isInstanceOf(TabStateException.class);
        assertThat(closed.total()).isEqualTo(Money.ZERO);
    }

    @Test
    void aMergedTabIsNoLongerOpen() {
        assertThat(TabStatus.MERGED.isOpen()).isFalse();
        assertThat(TabStatus.CLOSING.isOpen()).isTrue();
    }
}
