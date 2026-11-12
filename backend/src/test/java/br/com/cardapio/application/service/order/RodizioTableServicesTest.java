package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.in.order.TableOrders;
import br.com.cardapio.application.port.in.rodizio.ActivateRodizioCommand;
import br.com.cardapio.application.port.in.rodizio.RodizioQuote;
import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.service.call.InMemoryCalls;
import br.com.cardapio.application.service.rodizio.ActivateRodizioService;
import br.com.cardapio.application.service.rodizio.ChangeRodizioGuestsService;
import br.com.cardapio.application.service.rodizio.QuoteRodiziosService;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.exception.ProductNotInRodizioException;
import br.com.cardapio.domain.rodizio.exception.RodizioAlreadyActiveException;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TimedPrice;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.support.InMemoryRodizios;
import br.com.cardapio.support.InMemoryStorage;
import br.com.cardapio.support.InMemorySubscriptions;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import br.com.cardapio.application.service.rodizio.JoinRodizioService;
import br.com.cardapio.domain.rodizio.exception.RodizioNotJoinedException;
import br.com.cardapio.domain.rodizio.model.RodizioParticipation;
import br.com.cardapio.application.service.call.DeclineCallService;
import br.com.cardapio.application.service.call.AcceptCallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O rodízio na mesa: o garçom ativa, a mesa pede rodadas, e a conta soma pessoas × preço. */
class RodizioTableServicesTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();

    /** Sexta, 6 de novembro, 20h em São Paulo: vale o preço especial do jantar de fim de semana. */
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
    private final UserId carlos = UserId.newId();
    private DiningTable mesa04;
    private Product guioza;
    private Product sunomono;
    private Rodizio japones;

    @BeforeEach
    void aTableAndARodizio() {
        mesa04 = tables.given(sakura, "04", QR_04);
        sessions.givenPresent(sakura, mesa04.id(), "ana", NOW);
        guioza = fakes.product(sakura, "Guioza", 29_90, true);
        sunomono = fakes.product(sakura, "Sunomono", 19_90, true);
        TimedPrice weekend = new TimedPrice(Set.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY), LocalTime.of(18, 0),
                LocalTime.of(23, 59), Money.ofCents(149_90));
        japones = Rodizio.create(sakura, "Rodízio Japonês", RodizioType.A_LA_MINUTE, "", RodizioRules.NONE,
                List.of(new PriceTier("Adulto", Money.ofCents(129_90), List.of(weekend)),
                        new PriceTier("Criança", Money.ofCents(64_90), List.of())),
                Set.of(guioza.id()), NOW);
        rodizios.save(japones);
    }

    private TableRodizio activate(Rodizio rodizio, Map<String, Integer> guests) {
        return new ActivateRodizioService(tables, rodizios, id -> ZoneId.of("America/Sao_Paulo"), fakes, rodizios,
                calls, rodizios, published::add, clock)
                .activate(new ActivateRodizioCommand(sakura, mesa04.id(), rodizio.id(), guests, carlos));
    }

    private TableOrder send(boolean round, Product product, int quantity) {
        return send("ana", round, product, quantity);
    }

    private TableOrder send(String phone, boolean round, Product product, int quantity) {
        return new PlaceOrderService(tables, sessions, fakes, fakes, fakes, fakes, rodizios, rodizios, rodizios,
                subscriptions, published::add, clock).place(new PlaceOrderCommand(QR_04, phone,
                List.of(new OrderLine(product.id(), quantity, null)), round));
    }

    private TableOrders tableOrders() {
        return tableOrders("ana");
    }

    private TableOrders tableOrders(String phone) {
        return new ListTableOrdersService(tables, sessions, fakes, fakes, fakes, rodizios, rodizios, fakes, rodizios,
                clock).list(QR_04, phone);
    }

    /**
     * Quem chega depois da ativação não entra sozinho no rodízio (ele é cobrado por pessoa): o
     * celular pergunta, "Vou participar" chama o garçom, e só depois que ele ajusta as pessoas o
     * celular pede rodadas.
     */
    @Test
    void aPhoneThatArrivesLaterAsksAndJoinsWhenTheWaiterCountsIt() {
        activate(japones, Map.of("Adulto", 2));
        // A ana estava na mesa na ativação; a bia chega depois.
        rodizios.openToAll.clear();
        var tabRodizio = rodizios.byTab.values().iterator().next().id();
        rodizios.phones.put(tabRodizio, new java.util.HashMap<>(Map.of(sessions.byToken("ana").id(),
                RodizioParticipation.JOINED)));
        sessions.givenPresent(sakura, mesa04.id(), "bia", NOW);

        assertThat(tableOrders("ana").participation()).isEqualTo(RodizioParticipation.JOINED);
        assertThat(tableOrders("bia").participation()).isEqualTo(RodizioParticipation.NONE);
        assertThatThrownBy(() -> send("bia", true, guioza, 1)).isInstanceOf(RodizioNotJoinedException.class);

        JoinRodizioService join = new JoinRodizioService(tables, sessions, fakes, rodizios, rodizios, calls,
                published::add, clock);
        assertThat(join.join(QR_04, "bia")).isEqualTo(RodizioParticipation.REQUESTED);
        Call request = calls.findOpen(sakura, mesa04.id(), CallType.RODIZIO).orElseThrow();
        assertThat(tableOrders("bia").participation()).isEqualTo(RodizioParticipation.REQUESTED);
        assertThatThrownBy(() -> send("bia", true, guioza, 1)).isInstanceOf(RodizioNotJoinedException.class);

        // O garçom conta a bia: ela entra, e o "Quero o rodízio" termina atendido.
        new ChangeRodizioGuestsService(tables, fakes, rodizios, rodizios, calls, published::add, clock)
                .change(sakura, mesa04.id(), Map.of("Adulto", 3), carlos);

        assertThat(tableOrders("bia").participation()).isEqualTo(RodizioParticipation.JOINED);
        assertThat(calls.saved.get(request.id()).status()).isEqualTo(CallStatus.DONE);
        assertThat(send("bia", true, guioza, 1).items()).singleElement()
                .satisfies(i -> assertThat(i.rodizio()).isTrue());
        // Quem já estava dentro não pede de novo.
        assertThat(join.join(QR_04, "ana")).isEqualTo(RodizioParticipation.JOINED);
    }

    /** O garçom recusa o "Quero o rodízio" de quem chegou depois: o celular volta a não estar, e pode pedir de novo. */
    @Test
    void theWaiterDeclinesSomeoneWhoArrivedLater() {
        activate(japones, Map.of("Adulto", 2));
        rodizios.openToAll.clear();
        sessions.givenPresent(sakura, mesa04.id(), "bia", NOW);
        JoinRodizioService join = new JoinRodizioService(tables, sessions, fakes, rodizios, rodizios, calls,
                published::add, clock);
        join.join(QR_04, "bia");
        Call request = calls.findOpen(sakura, mesa04.id(), CallType.RODIZIO).orElseThrow();
        // Aceitar e depois recusar também vale: o rodízio se recusa enquanto o chamado está aberto.
        new AcceptCallService(calls, published::add, clock).accept(sakura, request.id(), carlos);

        new DeclineCallService(calls, fakes, rodizios, rodizios, published::add, clock).decline(sakura, request.id());

        assertThat(calls.saved.get(request.id()).status()).isEqualTo(CallStatus.CANCELED);
        assertThat(tableOrders("bia").participation()).isEqualTo(RodizioParticipation.NONE);
        assertThat(published).last().isInstanceOf(br.com.cardapio.domain.rodizio.event.RodizioChanged.class);
        assertThat(join.join(QR_04, "bia")).isEqualTo(RodizioParticipation.REQUESTED);
    }

    @Test
    void outsideTheRodizioThePhoneHasNoParticipation() {
        send(false, sunomono, 1);

        assertThat(tableOrders().participation()).isNull();
        assertThatThrownBy(() -> new JoinRodizioService(tables, sessions, fakes, rodizios, rodizios, calls,
                published::add, clock).join(QR_04, "ana")).isInstanceOf(NoRodizioOnTableException.class);
    }

    private Tab theTab() {
        return fakes.tabs.values().iterator().next();
    }

    @Test
    void theWaiterActivatesWithThePriceOfNowAndAnswersTheRequest() {
        Call request = Call.open(sakura, mesa04.id(), sessions.byToken("ana").id(), CallType.RODIZIO, null,
                NOW.minusSeconds(60));
        calls.insertIfNoneOpen(request);

        TableRodizio activated = activate(japones, Map.of("Adulto", 3, "Criança", 1));

        assertThat(activated.people()).isEqualTo(4);
        assertThat(activated.total()).isEqualTo(Money.ofCents(3 * 149_90 + 64_90));
        // A comanda abriu na ativação, antes de qualquer pedido.
        assertThat(fakes.tabs).hasSize(1);
        assertThat(calls.saved.get(request.id()).status()).isEqualTo(CallStatus.DONE);
        assertThat(calls.saved.get(request.id()).acceptedBy()).isEqualTo(carlos);
        assertThat(published).hasAtLeastOneElementOfType(CallChanged.class)
                .hasAtLeastOneElementOfType(RodizioChanged.class);
    }

    @Test
    void oneRodizioPerTabAndOnlyFromThisRestaurant() {
        activate(japones, Map.of("Adulto", 2));

        assertThatThrownBy(() -> activate(japones, Map.of("Adulto", 2)))
                .isInstanceOf(RodizioAlreadyActiveException.class).hasMessageContaining("Rodízio Japonês");
        Rodizio elsewhere = Rodizio.create(EstablishmentId.newId(), "Pizza", RodizioType.A_LA_MINUTE, "",
                RodizioRules.NONE, List.of(new PriceTier("Adulto", Money.ofCents(79_90), List.of())), Set.of(), NOW);
        rodizios.save(elsewhere);
        assertThatThrownBy(() -> activate(elsewhere, Map.of("Adulto", 2)))
                .isInstanceOf(RodizioNotFoundException.class);
        DiningTable theirs = tables.given(EstablishmentId.newId(), "04", "qr-de-outra-casa-00000000");
        assertThatThrownBy(() -> new ActivateRodizioService(tables, rodizios, id -> ZoneId.of("UTC"), fakes,
                rodizios, calls, rodizios, published::add, clock)
                .activate(new ActivateRodizioCommand(sakura, theirs.id(), japones.id(), Map.of("Adulto", 2), carlos)))
                .isInstanceOf(TableNotFoundException.class);
    }

    @Test
    void theRoundGoesToTheKitchenAtZeroAndTheCartKeepsItsPrice() {
        activate(japones, Map.of("Adulto", 3, "Criança", 1));

        TableOrder round = send(true, guioza, 4);
        TableOrder cart = send(false, sunomono, 1);

        assertThat(round.items()).singleElement().satisfies(i -> {
            assertThat(i.unitPrice()).isEqualTo(Money.ZERO);
            assertThat(i.rodizio()).isTrue();
        });
        assertThat(fakes.items.values()).filteredOn(OrderItem::inRodizio).singleElement()
                .satisfies(i -> assertThat(i.tabRodizioId()).isNotNull());
        assertThat(cart.items().getFirst().unitPrice()).isEqualTo(Money.ofCents(19_90));
        // Sunomono não é do rodízio: pela rodada, não vai.
        assertThatThrownBy(() -> send(true, sunomono, 1))
                .isInstanceOf(ProductNotInRodizioException.class).hasMessageContaining("Sunomono");

        TableOrders orders = tableOrders();
        assertThat(orders.rodizio().people()).isEqualTo(4);
        assertThat(orders.rodizioProducts()).containsExactly(guioza.id());
        assertThat(orders.total()).isEqualTo(Money.ofCents(3 * 149_90 + 64_90 + 19_90));
    }

    @Test
    void withoutTheRodizioThereIsNoRound() {
        assertThatThrownBy(() -> send(true, guioza, 1)).isInstanceOf(NoRodizioOnTableException.class);
        assertThat(fakes.tabs).isEmpty();
        assertThat(tableOrders().rodizio()).isNull();
        assertThat(tableOrders().rodizioProducts()).isEmpty();
    }

    @Test
    void someoneArrivesAndTheBillIsPeopleTimesPricePlusTheExtras() {
        activate(japones, Map.of("Adulto", 2));
        send(false, sunomono, 1);
        fakes.items.replaceAll((id, item) -> item.deliver(carlos, NOW));

        TableRodizio changed = new ChangeRodizioGuestsService(tables, fakes, rodizios, rodizios, calls, published::add, clock)
                .change(sakura, mesa04.id(), Map.of("Adulto", 2, "Criança", 1), carlos);
        StaffTab open = new GetTabService(fakes, fakes, calls, rodizios, fakes).get(sakura, theTab().id());
        StaffTab closed = new CloseTabService(fakes, fakes, calls, sessions, rodizios, fakes, published::add, clock)
                .close(sakura, theTab().id(), carlos, PaymentMethod.PIX);

        long expected = 2 * 149_90 + 64_90 + 19_90;
        assertThat(changed.people()).isEqualTo(3);
        assertThat(open.rodizio().people()).isEqualTo(3);
        assertThat(open.total()).isEqualTo(Money.ofCents(expected));
        assertThat(closed.total()).isEqualTo(Money.ofCents(expected));
        assertThat(fakes.tabs.get(theTab().id()).total()).isEqualTo(Money.ofCents(expected));
        assertThatThrownBy(() -> new ChangeRodizioGuestsService(tables, fakes, rodizios, rodizios, calls, published::add, clock)
                .change(sakura, mesa04.id(), Map.of("Adulto", 1), carlos))
                .isInstanceOf(NoRodizioOnTableException.class);
    }

    @Test
    void theTableSeesThePricesOfNowInTheRestaurantClock() {
        var quotes = new QuoteRodiziosService(rodizios, id -> ZoneId.of("America/Sao_Paulo"), tables, sessions,
                new InMemoryStorage(), clock).forTable(QR_04, "ana", new PageQuery(0, 20));
        var inUtc = new QuoteRodiziosService(rodizios, id -> ZoneId.of("UTC"), tables, sessions, new InMemoryStorage(),
                clock)
                .forStaff(sakura, new PageQuery(0, 20));

        assertThat(quotes.content()).singleElement().satisfies(q -> assertThat(q.tiers())
                .extracting(RodizioQuote.Tier::price)
                .containsExactly(Money.ofCents(149_90), Money.ofCents(64_90)));
        // Em UTC já seria 23h de sexta: o jantar ainda vale. O relógio errado mudaria o preço perto da meia-noite.
        assertThat(inUtc.content().getFirst().tiers().getFirst().price()).isEqualTo(Money.ofCents(149_90));
    }
}
