package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.RodizioForm;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.exception.RodizioLimitReachedException;
import br.com.cardapio.domain.rodizio.exception.RodizioNameTakenException;
import br.com.cardapio.domain.rodizio.exception.RodizioNotFoundException;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RodizioServicesTest {

    private final RodizioFakes fakes = new RodizioFakes();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);
    private final ManageRodiziosService manage = new ManageRodiziosService(fakes, fakes.products, clock);
    private final ListRodiziosService list = new ListRodiziosService(fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    private static RodizioForm form(String name, Set<ProductId> products) {
        return new RodizioForm(name, RodizioType.A_LA_MINUTE, "", RodizioRules.NONE,
                List.of(new PriceTier("Adulto", Money.ofCents(12990), List.of()),
                        new PriceTier("Criança", Money.ofCents(6490), List.of())),
                products);
    }

    @Test
    void createsAndListsByName() {
        ProductId guioza = fakes.products.given(sakura, "Guioza").id();
        manage.create(sakura, form("Rodízio Pizza", Set.of()));
        Rodizio japones = manage.create(sakura, form("Rodízio Japonês", Set.of(guioza)));

        assertThat(japones.createdAt()).isEqualTo(clock.instant());
        assertThat(list.get(sakura, japones.id()).productIds()).containsExactly(guioza);
        assertThat(list.list(sakura, new PageQuery(0, 20)).content()).extracting(Rodizio::name)
                .containsExactly("Rodízio Japonês", "Rodízio Pizza");
    }

    @Test
    void namesDoNotRepeatAmongActiveRodiziosButEditingItselfIsFine() {
        Rodizio japones = manage.create(sakura, form("Japonês", Set.of()));
        manage.create(sakura, form("Pizza", Set.of()));

        assertThatThrownBy(() -> manage.create(sakura, form(" JAPONÊS ", Set.of())))
                .isInstanceOf(RodizioNameTakenException.class);
        assertThatThrownBy(() -> manage.update(sakura, japones.id(), form("pizza", Set.of())))
                .isInstanceOf(RodizioNameTakenException.class);
        assertThat(manage.update(sakura, japones.id(), form("Japonês", Set.of())).name()).isEqualTo("Japonês");

        manage.deactivate(sakura, japones.id());
        assertThat(manage.create(sakura, form("Japonês", Set.of())).active()).isTrue(); // o nome ficou livre
    }

    @Test
    void productsMustBeActiveInThisRestaurantsMenu() {
        ProductId elsewhere = fakes.products.given(EstablishmentId.newId(), "Guioza").id();

        assertThatThrownBy(() -> manage.create(sakura, form("Japonês", Set.of(elsewhere))))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("não está no cardápio");
        assertThatThrownBy(() -> manage.create(sakura, form("Japonês", Set.of(ProductId.newId()))))
                .isInstanceOf(InvalidValueException.class);
        assertThat(fakes.rodizios).isEmpty();
    }

    @Test
    void tenActiveRodiziosAtMost() {
        for (int i = 0; i < Rodizio.MAX_PER_ESTABLISHMENT; i++) {
            manage.create(sakura, form("Rodízio " + i, Set.of()));
        }

        assertThatThrownBy(() -> manage.create(sakura, form("Mais um", Set.of())))
                .isInstanceOf(RodizioLimitReachedException.class);
        assertThat(manage.create(EstablishmentId.newId(), form("Mais um", Set.of())).active()).isTrue();
    }

    @Test
    void deactivatedOrFromAnotherRestaurantIsNotFound() {
        Rodizio japones = manage.create(sakura, form("Japonês", Set.of()));
        EstablishmentId other = EstablishmentId.newId();

        assertThatThrownBy(() -> list.get(other, japones.id())).isInstanceOf(RodizioNotFoundException.class);
        assertThatThrownBy(() -> manage.update(other, japones.id(), form("Meu", Set.of())))
                .isInstanceOf(RodizioNotFoundException.class);
        assertThatThrownBy(() -> manage.deactivate(other, japones.id())).isInstanceOf(RodizioNotFoundException.class);

        manage.deactivate(sakura, japones.id());
        manage.deactivate(sakura, japones.id()); // de novo: tudo bem
        assertThatThrownBy(() -> list.get(sakura, japones.id())).isInstanceOf(RodizioNotFoundException.class);
        assertThatThrownBy(() -> manage.update(sakura, japones.id(), form("Japonês", Set.of())))
                .isInstanceOf(RodizioNotFoundException.class);
        assertThat(list.list(sakura, new PageQuery(0, 20)).content()).isEmpty();
    }
}
