package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.exception.CounterNotAllowedException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.floor.model.TableKind;
import br.com.cardapio.domain.floor.model.TableToken;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O balcão e a comanda do balcão (ADR 0015). */
class CounterTabTest {

    private static final EstablishmentId SAKURA = EstablishmentId.newId();
    private static final Instant NOW = Instant.parse("2026-11-25T23:00:00Z");
    private static final TableToken TOKEN = new TableToken("x".repeat(43));

    @Test
    void theCounterHasALongerNameAndNoQrCode() {
        DiningTable counter = DiningTable.createCounter(SAKURA, SectorId.newId(), "  Balcão   da varanda ", TOKEN, NOW);
        assertThat(counter.kind()).isEqualTo(TableKind.COUNTER);
        assertThat(counter.isCounter()).isTrue();
        assertThat(counter.label()).isEqualTo("Balcão da varanda");
        assertThatThrownBy(() -> counter.rotateToken(TOKEN, NOW)).isInstanceOf(CounterNotAllowedException.class)
                .hasMessage("O balcão não tem QR Code");
        assertThatThrownBy(() -> DiningTable.createCounter(SAKURA, SectorId.newId(), "B".repeat(31), TOKEN, NOW))
                .isInstanceOf(InvalidValueException.class);
        // A mesa continua com até 10.
        assertThatThrownBy(() -> DiningTable.create(SAKURA, SectorId.newId(), "Balcão do bar", TOKEN, NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThat(counter.moveTo(SectorId.newId()).isCounter()).isTrue();
    }

    @Test
    void theGuestNeedsANameAndAnOptionalCard() {
        CounterGuest guest = new CounterGuest("  camisa   azul ", "  ");
        assertThat(guest.name()).isEqualTo("camisa azul");
        assertThat(guest.card()).isNull();
        assertThat(new CounterGuest("João", " 12 ").card()).isEqualTo("12");
        assertThatThrownBy(() -> new CounterGuest(" ", null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new CounterGuest("x".repeat(41), null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new CounterGuest("João", "1 2")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new CounterGuest("João", "12345678901")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theCounterTabOpensWithTheNameAndCanBeRenamed() {
        DiningTableId counter = DiningTableId.newId();
        Tab tab = Tab.openAtCounter(SAKURA, counter, new CounterGuest("João", "12"), NOW);
        assertThat(tab.atCounter()).isTrue();
        assertThat(tab.status()).isEqualTo(TabStatus.OPEN);
        assertThat(tab.tableId()).isEqualTo(counter);

        Tab renamed = tab.rename(new CounterGuest("João Pedro", null));
        assertThat(renamed.guest()).isEqualTo(new CounterGuest("João Pedro", null));
        assertThat(renamed.atCounter()).isTrue();
        // A comanda de mesa não tem nome para corrigir.
        assertThatThrownBy(() -> Tab.open(SAKURA, DiningTableId.newId(), NOW).rename(new CounterGuest("Ana", null)))
                .isInstanceOf(TabStateException.class);
        assertThatThrownBy(() -> new Tab(TabId.newId(), SAKURA, counter, TabStatus.OPEN, NOW, null, null, null, null,
                null, null, true, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void goingToATableTheTabLeavesTheCounterAndKeepsTheName() {
        Tab tab = Tab.openAtCounter(SAKURA, DiningTableId.newId(), new CounterGuest("João", "12"), NOW);
        DiningTableId mesa = DiningTableId.newId();
        TabMove move = TabMove.toFreeTable(tab, mesa, UserId.newId(), NOW.plusSeconds(600));
        Tab moved = move.applyTo(tab, null);
        assertThat(moved.atCounter()).isFalse();
        assertThat(moved.tableId()).isEqualTo(mesa);
        assertThat(moved.guest()).isEqualTo(new CounterGuest("João", "12"));
        assertThat(moved.openedAt()).isEqualTo(NOW);

        // Fechada, a comanda não muda mais de nome.
        Tab closed = new Tab(moved.id(), SAKURA, mesa, TabStatus.CLOSED, NOW, NOW, null, null, null, null, null,
                false, moved.guest());
        assertThatThrownBy(() -> closed.rename(new CounterGuest("Ana", null))).isInstanceOf(TabStateException.class);
    }
}
