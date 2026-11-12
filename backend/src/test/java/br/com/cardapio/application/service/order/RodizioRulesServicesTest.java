package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.application.port.in.order.TableOrders;
import br.com.cardapio.application.port.in.rodizio.ActivateRodizioCommand;
import br.com.cardapio.application.service.call.InMemoryCalls;
import br.com.cardapio.application.service.rodizio.ActivateRodizioService;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.rodizio.exception.ProductOnlyInRodizioException;
import br.com.cardapio.domain.rodizio.exception.RoundNotAllowedException;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
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

/** As regras opcionais valendo nas rodadas, e o produto "só do rodízio" fora do carrinho. */
class RodizioRulesServicesTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();

    private static final Instant NOW = Instant.parse("2026-11-06T23:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";

    private final OrderFakes fakes = new OrderFakes();
    private final InMemoryRodizios rodizios = new InMemoryRodizios();
    private final InMemoryCalls calls = new InMemoryCalls();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final List<DomainEvent> published = new ArrayList<>();
    private final EstablishmentId sakura = EstablishmentId.newId();
    private DiningTable mesa04;
    private Product guioza;
    private Product sashimi;
    private Product coca;

    @BeforeEach
    void aTableAndARodizioWithRules() {
        mesa04 = tables.given(sakura, "04", QR_04);
        sessions.givenPresent(sakura, mesa04.id(), "ana", NOW);
        guioza = fakes.product(sakura, "Guioza", 29_90, true);
        coca = fakes.product(sakura, "Coca-Cola", 7_90, true);
        // O sashimi é só do rodízio: fica fora do cardápio à la carte.
        sashimi = Product.create(sakura, MenuCategoryId.newId(),
                new ProductDetails("Sashimi", "", Money.ofCents(49_90), null, false, false), true, 9);
        fakes.products.put(sashimi.id(), sashimi);
        // 2 de cada item por pessoa, até 3 itens não entregues por pessoa, 10 minutos entre as rodadas.
        Rodizio japones = Rodizio.create(sakura, "Rodízio Japonês", RodizioType.A_LA_MINUTE, "",
                new RodizioRules(2, 3, 10, null, "Sobras são cobradas à parte."),
                List.of(new PriceTier("Adulto", Money.ofCents(129_90), List.of())), Set.of(guioza.id(), sashimi.id()),
                NOW);
        rodizios.save(japones);
        new ActivateRodizioService(tables, rodizios, id -> ZoneId.of("America/Sao_Paulo"), fakes, rodizios, calls, rodizios,
                published::add, clock(NOW)).activate(new ActivateRodizioCommand(sakura, mesa04.id(), japones.id(),
                Map.of("Adulto", 2), UserId.newId()));
    }

    private static Clock clock(Instant at) {
        return Clock.fixed(at, ZoneOffset.UTC);
    }

    private TableOrder send(boolean round, Instant at, Product product, int quantity) {
        return new PlaceOrderService(tables, sessions, fakes, fakes, fakes, fakes, rodizios, rodizios, rodizios, subscriptions,
                published::add, clock(at)).place(new PlaceOrderCommand(QR_04, "ana",
                List.of(new OrderLine(product.id(), quantity, null)), round));
    }

    private TableOrders tableOrders(Instant at) {
        return new ListTableOrdersService(tables, sessions, fakes, fakes, fakes, rodizios, rodizios, fakes, rodizios, clock(at))
                .list(QR_04, "ana");
    }

    @Test
    void theRoundsFollowTheRulesTimesThePeople() {
        // 2 por pessoa × 2 pessoas = 4 de cada item.
        assertThatThrownBy(() -> send(true, NOW, guioza, 5))
                .isInstanceOf(RoundNotAllowedException.class).hasMessageContaining("até 4 de Guioza");
        send(true, NOW, guioza, 4);

        var limits = tableOrders(NOW).rodizioLimits();
        assertThat(limits.maxPerItem()).isEqualTo(4);
        assertThat(limits.room()).isEqualTo(2);
        assertThat(limits.nextRoundAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(limits.wasteNotice()).isEqualTo("Sobras são cobradas à parte.");

        // Antes dos 10 minutos, não vai; depois, cabem só mais 2 (6 não entregues no máximo, 4 a caminho).
        assertThatThrownBy(() -> send(true, NOW.plusSeconds(300), sashimi, 1))
                .isInstanceOf(RoundNotAllowedException.class).hasMessage("A próxima rodada libera em 5 minutos.");
        Instant later = NOW.plusSeconds(601);
        assertThatThrownBy(() -> send(true, later, sashimi, 3))
                .isInstanceOf(RoundNotAllowedException.class).hasMessageContaining("cabem só mais 2 itens");
        send(true, later, sashimi, 2);

        // Chegou tudo à mesa: a próxima rodada tem espaço de novo.
        fakes.items.replaceAll((id, item) -> item.deliver(UserId.newId(), later));
        assertThat(tableOrders(later.plusSeconds(601)).rodizioLimits().room()).isEqualTo(6);
        send(true, later.plusSeconds(601), guioza, 4);
    }

    @Test
    void theCartRefusesWhatIsOnlyInTheRodizio() {
        assertThatThrownBy(() -> send(false, NOW, sashimi, 1))
                .isInstanceOf(ProductOnlyInRodizioException.class)
                .hasMessage("Sashimi é só do rodízio. Peça pela aba Rodízio.");
        assertThat(send(false, NOW, coca, 2).items().getFirst().unitPrice()).isEqualTo(Money.ofCents(7_90));
        assertThat(send(true, NOW, sashimi, 1).items().getFirst().unitPrice()).isEqualTo(Money.ZERO);
        // A Coca à parte não conta nas regras do rodízio.
        assertThat(tableOrders(NOW).rodizioLimits().room()).isEqualTo(5);
    }
}
