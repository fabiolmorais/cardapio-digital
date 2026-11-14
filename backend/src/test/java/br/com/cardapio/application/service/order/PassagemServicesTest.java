package br.com.cardapio.application.service.order;

import br.com.cardapio.support.InMemoryProductOptions;
import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.rodizio.ActivateRodizioCommand;
import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.service.call.InMemoryCalls;
import br.com.cardapio.application.service.rodizio.ActivateRodizioService;
import br.com.cardapio.application.service.rodizio.TurnRodizioCardService;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.plan.exception.ServiceSuspendedException;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.exception.RodizioCardException;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.support.InMemoryRodizios;
import br.com.cardapio.support.InMemorySubscriptions;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O rodízio de passagem na mesa: a ativação, o cartão verde/vermelho e a rodada, que não vale. */
class PassagemServicesTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();

    private static final Instant NOW = Instant.parse("2026-11-06T23:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";

    private final OrderFakes fakes = new OrderFakes();
    private final InMemoryRodizios rodizios = new InMemoryRodizios();
    private final InMemoryCalls calls = new InMemoryCalls();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final List<DomainEvent> published = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private DiningTable mesa04;
    private Product picanha;

    @BeforeEach
    void aTable() {
        mesa04 = tables.given(sakura, "04", QR_04);
        sessions.givenPresent(sakura, mesa04.id(), "ana", NOW);
        picanha = fakes.product(sakura, "Picanha", 0, true);
    }

    private Rodizio rodizio(RodizioType type) {
        Rodizio rodizio = Rodizio.create(sakura, "Churrasco", type, "", RodizioRules.NONE,
                List.of(new PriceTier("Adulto", Money.ofCents(159_90), List.of())), Set.of(picanha.id()), NOW);
        rodizios.save(rodizio);
        new ActivateRodizioService(tables, rodizios, id -> ZoneId.of("America/Sao_Paulo"), fakes, rodizios, calls, rodizios,
                published::add, clock).activate(new ActivateRodizioCommand(sakura, mesa04.id(), rodizio.id(),
                Map.of("Adulto", 2), UserId.newId()));
        published.clear();
        return rodizio;
    }

    private TableRodizio turn(String phone, TabRodizioStatus status) {
        return new TurnRodizioCardService(tables, sessions, fakes, rodizios, subscriptions, published::add, clock)
                .turn(QR_04, phone, status);
    }

    @Test
    void thePassagemActivatesAndThePhoneTurnsTheCard() {
        rodizio(RodizioType.PASSAGEM);

        TableRodizio paused = turn("ana", TabRodizioStatus.PAUSED);

        assertThat(paused.type()).isEqualTo(RodizioType.PASSAGEM);
        assertThat(paused.status()).isEqualTo(TabRodizioStatus.PAUSED);
        assertThat(published).singleElement().isInstanceOf(RodizioChanged.class);
        // A mesma cor de novo não avisa ninguém.
        turn("ana", TabRodizioStatus.PAUSED);
        assertThat(published).hasSize(1);
        assertThat(turn("ana", TabRodizioStatus.SERVING).status()).isEqualTo(TabRodizioStatus.SERVING);
    }

    @Test
    void theCutRequestIsARoundAtZeroWithThePoint() {
        rodizio(RodizioType.PASSAGEM);

        var cut = new PlaceOrderService(tables, sessions, fakes, fakes, new InMemoryProductOptions(), fakes, fakes, rodizios, rodizios, rodizios, subscriptions,
                published::add, clock).place(new PlaceOrderCommand(QR_04, "ana",
                List.of(new OrderLine(picanha.id(), 1, "Mal passada")), true));

        assertThat(cut.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("Picanha");
            assertThat(item.note()).isEqualTo("Mal passada");
            assertThat(item.unitPrice()).isEqualTo(Money.ZERO);
            assertThat(item.rodizio()).isTrue();
        });
    }

    @Test
    void theCardNeedsThePassagemThePresenceAndTheRodizio() {
        assertThatThrownBy(() -> turn("ana", TabRodizioStatus.PAUSED)).isInstanceOf(NoRodizioOnTableException.class);
        rodizio(RodizioType.A_LA_MINUTE);
        assertThatThrownBy(() -> turn("ana", TabRodizioStatus.PAUSED)).isInstanceOf(RodizioCardException.class);
        sessions.given(sakura, mesa04.id(), "bia", NOW);
        assertThatThrownBy(() -> turn("bia", TabRodizioStatus.PAUSED))
                .isInstanceOf(PresenceRequiredException.class);
    }

    @Test
    void aSuspendedSubscriptionStopsTheCard() {
        rodizio(RodizioType.PASSAGEM);
        subscriptions.suspended();

        assertThatThrownBy(() -> turn("ana", TabRodizioStatus.PAUSED)).isInstanceOf(ServiceSuspendedException.class);
        assertThat(published).isEmpty();
    }
}
