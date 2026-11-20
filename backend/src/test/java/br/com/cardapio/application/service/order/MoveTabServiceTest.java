package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.MoveTabCommand;
import br.com.cardapio.application.port.in.order.MovedTab;
import br.com.cardapio.application.service.call.InMemoryCalls;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.order.event.TabMoved;
import br.com.cardapio.domain.order.exception.TabMoveException;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabStatus;
import br.com.cardapio.domain.rodizio.model.GuestTier;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import br.com.cardapio.support.InMemoryRodizios;
import br.com.cardapio.support.InMemoryTabMoves;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mudar a comanda de mesa e juntar comandas (ADR 0012), sem banco. */
class MoveTabServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T23:00:00Z");
    private static final LocalDateTime FRIDAY_NIGHT = LocalDateTime.of(2026, 11, 20, 20, 0);

    private final OrderFakes fakes = new OrderFakes();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryRodizios rodizios = new InMemoryRodizios();
    private final InMemoryCalls calls = new InMemoryCalls();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final InMemoryTabMoves moves = new InMemoryTabMoves();
    private final List<DomainEvent> published = new ArrayList<>();
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final UserId joao = UserId.newId();
    private DiningTable mesa04;
    private DiningTable mesa05;

    @BeforeEach
    void twoTables() {
        mesa04 = tables.given(sakura, "04", "qr-da-mesa-04-00000000000");
        mesa05 = tables.given(sakura, "05", "qr-da-mesa-05-00000000000");
    }

    private MoveTabService service() {
        return new MoveTabService(fakes, tables, fakes, fakes, rodizios, rodizios, calls, sessions, moves,
                published::add, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private MovedTab move(Tab tab, DiningTable to) {
        return service().move(new MoveTabCommand(sakura, joao, tab.id(), to.id()));
    }

    private Tab open(DiningTable table, Instant openedAt) {
        Tab tab = Tab.open(sakura, table.id(), openedAt);
        fakes.tabs.put(tab.id(), tab);
        return tab;
    }

    private Rodizio rodizio(String name) {
        Rodizio rodizio = Rodizio.create(sakura, name, RodizioType.A_LA_MINUTE, "", RodizioRules.NONE,
                List.of(new PriceTier("Adulto", Money.ofCents(129_90), List.of())), Set.of(), NOW);
        rodizios.save(rodizio);
        return rodizio;
    }

    private TabRodizio onRodizio(Tab tab, Rodizio rodizio, int adults) {
        TabRodizio active = TabRodizio.activate(rodizio, tab.id(), Map.of("Adulto", adults), FRIDAY_NIGHT, joao, NOW);
        rodizios.byTab.put(tab.id(), active);
        return active;
    }

    @Test
    void toAFreeTableTheTabAndThePhonesGoAlong() {
        Tab tab = open(mesa04, NOW.minusSeconds(3600));
        TableSession ana = sessions.givenPresent(sakura, mesa04.id(), "ana", NOW.minusSeconds(600));

        MovedTab moved = move(tab, mesa05);

        assertThat(moved.tabId()).isEqualTo(tab.id());
        assertThat(moved.merged()).isFalse();
        assertThat(fakes.tabs.get(tab.id()).tableId()).isEqualTo(mesa05.id());
        assertThat(sessions.saved.get(ana.tokenHash()).tableId()).isEqualTo(mesa05.id());
        assertThat(moves.saved).hasSize(1);
        // Um aviso para cada mesa: os celulares da 04 vão atrás, os da 05 atualizam.
        assertThat(published).filteredOn(TabMoved.class::isInstance).hasSize(2);
    }

    @Test
    void onlyTheSourceHadRodizioAndTheMergedTabKeepsIt() {
        Tab from = open(mesa04, NOW.minusSeconds(3600));
        Tab into = open(mesa05, NOW.minusSeconds(600));
        TabRodizio japones = onRodizio(from, rodizio("Japonês"), 2);

        MovedTab moved = move(from, mesa05);

        assertThat(moved.tabId()).isEqualTo(into.id());
        assertThat(rodizios.findByTab(sakura, into.id())).hasValueSatisfying(r -> {
            assertThat(r.id()).isEqualTo(japones.id());
            assertThat(r.people()).isEqualTo(2);
        });
        assertThat(fakes.tabs.get(from.id()).status()).isEqualTo(TabStatus.MERGED);
        // A junta fica aberta desde a mais antiga das duas.
        assertThat(fakes.tabs.get(into.id()).openedAt()).isEqualTo(from.openedAt());
    }

    @Test
    void theSameRodizioOnBothAddsThePeople() {
        Rodizio japones = rodizio("Japonês");
        Tab from = open(mesa04, NOW.minusSeconds(3600));
        Tab into = open(mesa05, NOW.minusSeconds(600));
        onRodizio(from, japones, 2);
        onRodizio(into, japones, 3);

        move(from, mesa05);

        assertThat(rodizios.findByTab(sakura, into.id()).orElseThrow().guests())
                .containsExactly(new GuestTier("Adulto", 5, Money.ofCents(129_90)));
    }

    @Test
    void differentRodiziosAreRefusedAndNothingChanges() {
        Tab from = open(mesa04, NOW.minusSeconds(3600));
        Tab into = open(mesa05, NOW.minusSeconds(600));
        onRodizio(from, rodizio("Japonês"), 2);
        onRodizio(into, rodizio("Churrasco"), 3);

        assertThatThrownBy(() -> move(from, mesa05)).isInstanceOf(TabMoveException.class)
                .hasMessageContaining("rodízios diferentes");
        assertThat(fakes.tabs.get(from.id()).status()).isEqualTo(TabStatus.OPEN);
        assertThat(fakes.tabs.get(from.id()).tableId()).isEqualTo(mesa04.id());
        assertThat(moves.saved).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void bothTablesAskedForTheBillAndTheOlderCallStays() {
        Tab from = open(mesa04, NOW.minusSeconds(3600));
        open(mesa05, NOW.minusSeconds(600));
        Call older = Call.open(sakura, mesa04.id(), TableSessionId.newId(), CallType.BILL, PaymentMethod.PIX,
                NOW.minusSeconds(300));
        Call newer = Call.open(sakura, mesa05.id(), TableSessionId.newId(), CallType.BILL, PaymentMethod.CARD,
                NOW.minusSeconds(60));
        calls.saved.put(older.id(), older);
        calls.saved.put(newer.id(), newer);

        move(from, mesa05);

        assertThat(calls.saved.get(newer.id()).status()).isEqualTo(CallStatus.DONE);
        assertThat(calls.saved.get(older.id()).status()).isEqualTo(CallStatus.PENDING);
        assertThat(calls.saved.get(older.id()).tableId()).isEqualTo(mesa05.id());
    }
}
