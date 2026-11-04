package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.exception.RodizioNotServedException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabRodizioTest {

    private static final Instant NOW = Instant.parse("2026-11-06T23:00:00Z");
    /** Sexta, 6 de novembro, 20h no relógio do restaurante (23h em UTC). */
    private static final LocalDateTime FRIDAY_NIGHT = LocalDateTime.of(2026, 11, 6, 20, 0);
    /** Segunda, 9 de novembro, 20h. */
    private static final LocalDateTime MONDAY_NIGHT = LocalDateTime.of(2026, 11, 9, 20, 0);

    private final EstablishmentId sakura = EstablishmentId.newId();
    private final TabId tab = TabId.newId();
    private final UserId carlos = UserId.newId();

    private Rodizio japones(RodizioType type) {
        TimedPrice weekend = new TimedPrice(Set.of(FRIDAY, SATURDAY), LocalTime.of(18, 0), LocalTime.of(23, 59),
                Money.ofCents(149_90));
        return Rodizio.create(sakura, "Rodízio Japonês", type, "", RodizioRules.NONE,
                List.of(new PriceTier("Adulto", Money.ofCents(129_90), List.of(weekend)),
                        new PriceTier("Criança", Money.ofCents(64_90), List.of())),
                Set.of(), NOW);
    }

    @Test
    void theWaiterConfirmsThePeopleAndThePriceOfNowIsCopied() {
        TabRodizio friday = TabRodizio.activate(japones(RodizioType.A_LA_MINUTE), tab,
                Map.of("Adulto", 3, "Criança", 1), FRIDAY_NIGHT, carlos, NOW);
        TabRodizio monday = TabRodizio.activate(japones(RodizioType.A_LA_MINUTE), tab, Map.of("Adulto", 3),
                MONDAY_NIGHT, carlos, NOW);

        assertThat(friday.rodizioName()).isEqualTo("Rodízio Japonês");
        assertThat(friday.people()).isEqualTo(4);
        assertThat(friday.total()).isEqualTo(Money.ofCents(3 * 149_90 + 64_90));
        assertThat(friday.activatedBy()).isEqualTo(carlos);
        // A faixa que não veio fica com 0, mas com o preço copiado (alguém pode chegar depois).
        assertThat(monday.guests()).containsExactly(new GuestTier("Adulto", 3, Money.ofCents(129_90)),
                new GuestTier("Criança", 0, Money.ofCents(64_90)));
        assertThat(monday.total()).isEqualTo(Money.ofCents(3 * 129_90));
    }

    @Test
    void someoneArrivesAndThePricesStayTheOnesFromTheActivation() {
        TabRodizio friday = TabRodizio.activate(japones(RodizioType.A_LA_MINUTE), tab, Map.of("Adulto", 2),
                FRIDAY_NIGHT, carlos, NOW);

        TabRodizio changed = friday.changeGuests(Map.of("Adulto", 2, "Criança", 1));

        assertThat(changed.id()).isEqualTo(friday.id());
        assertThat(changed.people()).isEqualTo(3);
        assertThat(changed.total()).isEqualTo(Money.ofCents(2 * 149_90 + 64_90));
        assertThatThrownBy(() -> friday.changeGuests(Map.of("Adulto", 0)))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("pelo menos 1 pessoa");
    }

    @Test
    void needsSomeoneKnownTiersAndAnActiveALaMinuteRodizio() {
        Rodizio rodizio = japones(RodizioType.A_LA_MINUTE);

        assertThatThrownBy(() -> TabRodizio.activate(rodizio, tab, Map.of(), FRIDAY_NIGHT, carlos, NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> TabRodizio.activate(rodizio, tab, Map.of("Sênior", 2), FRIDAY_NIGHT, carlos, NOW))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("Atualize a tela");
        assertThatThrownBy(() -> TabRodizio.activate(rodizio, tab, Map.of("Adulto", 100), FRIDAY_NIGHT, carlos,
                NOW)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> TabRodizio.activate(rodizio.deactivate(), tab, Map.of("Adulto", 2), FRIDAY_NIGHT,
                carlos, NOW)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> TabRodizio.activate(japones(RodizioType.PASSAGEM), tab, Map.of("Adulto", 2),
                FRIDAY_NIGHT, carlos, NOW)).isInstanceOf(RodizioNotServedException.class);
    }
}
