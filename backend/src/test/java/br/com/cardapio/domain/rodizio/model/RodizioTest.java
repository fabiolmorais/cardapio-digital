package br.com.cardapio.domain.rodizio.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RodizioTest {

    private final EstablishmentId sakura = EstablishmentId.newId();
    private final Instant now = Instant.parse("2026-11-04T15:00:00Z");

    private static PriceTier tier(String name, long cents) {
        return new PriceTier(name, Money.ofCents(cents), List.of());
    }

    private Rodizio japones(List<PriceTier> tiers) {
        return Rodizio.create(sakura, " Rodízio Japonês ", RodizioType.A_LA_MINUTE, null, RodizioRules.NONE, tiers,
                Set.of(ProductId.newId()), now);
    }

    @Test
    void createStartsActiveWithNoRules() {
        Rodizio rodizio = japones(List.of(tier("Adulto", 12990), tier("Criança", 6490)));

        assertThat(rodizio.name()).isEqualTo("Rodízio Japonês");
        assertThat(rodizio.description()).isEmpty();
        assertThat(rodizio.active()).isTrue();
        assertThat(rodizio.rules()).isEqualTo(RodizioRules.NONE);
        assertThat(rodizio.tiers()).extracting(PriceTier::name).containsExactly("Adulto", "Criança");
    }

    @Test
    void needsOneToSixTiersWithDifferentNames() {
        assertThatThrownBy(() -> japones(List.of())).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> japones(List.of(tier("A", 1), tier("B", 1), tier("C", 1), tier("D", 1),
                tier("E", 1), tier("F", 1), tier("G", 1)))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> japones(List.of(tier("Adulto", 1), tier("ADULTO", 2))))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("Duas faixas");
    }

    @Test
    void editKeepsTheIdAndDeactivateKeepsTheRest() {
        Rodizio rodizio = japones(List.of(tier("Adulto", 12990)));
        RodizioRules rules = new RodizioRules(3, null, 10, 120, "Sobras são cobradas");

        Rodizio edited = rodizio.edit("Japonês Premium", RodizioType.A_LA_MINUTE, "Peças especiais", rules,
                List.of(tier("Adulto", 15990)), Set.of());
        Rodizio off = edited.deactivate();

        assertThat(off.id()).isEqualTo(rodizio.id());
        assertThat(off.name()).isEqualTo("Japonês Premium");
        assertThat(off.rules()).isEqualTo(rules);
        assertThat(off.productIds()).isEmpty();
        assertThat(off.active()).isFalse();
        assertThat(off.createdAt()).isEqualTo(now);
    }

    @Test
    void rulesAreOptionalButStayInRange() {
        assertThat(new RodizioRules(null, null, null, null, "  ").wasteNotice()).isNull();
        assertThatThrownBy(() -> new RodizioRules(0, null, null, null, null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RodizioRules(null, 101, null, null, null))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RodizioRules(null, null, 121, null, null))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RodizioRules(null, null, null, 14, null))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RodizioRules(null, null, null, null, "x".repeat(201)))
                .isInstanceOf(InvalidValueException.class);
    }
}
