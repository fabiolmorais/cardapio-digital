package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.SectorSummary;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorNameTakenException;
import br.com.cardapio.domain.floor.exception.SectorNotEmptyException;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.model.Sector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectorServicesTest {

    private final FloorFakes fakes = new FloorFakes();
    private final ListSectorsService list = new ListSectorsService(fakes);
    private final CreateSectorService create = new CreateSectorService(fakes);
    private final RenameSectorService rename = new RenameSectorService(fakes);
    private final DeactivateSectorService deactivate = new DeactivateSectorService(fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void newSectorsGoToTheEndOfTheList() {
        create.create(sakura, "Salão principal");
        SectorSummary deck = create.create(sakura, "Deck");

        assertThat(deck.position()).isEqualTo(1);
        assertThat(list.list(sakura, new PageQuery(0, 20)).content())
                .extracting(SectorSummary::name).containsExactly("Salão principal", "Deck");
    }

    @Test
    void namesDoNotRepeatIgnoringCaseButOtherRestaurantsAreFree() {
        create.create(sakura, "Deck");

        assertThatThrownBy(() -> create.create(sakura, " deck ")).isInstanceOf(SectorNameTakenException.class);
        assertThat(create.create(EstablishmentId.newId(), "Deck").name()).isEqualTo("Deck");
    }

    @Test
    void renameChecksOtherNamesButNotItself() {
        create.create(sakura, "Deck");
        SectorSummary salao = create.create(sakura, "Salão");

        assertThat(rename.rename(sakura, salao.id(), "SALÃO").name()).isEqualTo("SALÃO");
        assertThatThrownBy(() -> rename.rename(sakura, salao.id(), "Deck"))
                .isInstanceOf(SectorNameTakenException.class);
    }

    @Test
    void sectorWithActiveTablesCannotBeDeactivated() {
        Sector deck = fakes.givenSector(sakura, "Deck");
        var table = fakes.givenTable(deck, "20");

        assertThatThrownBy(() -> deactivate.deactivate(sakura, deck.id()))
                .isInstanceOf(SectorNotEmptyException.class)
                .hasMessageContaining("1 mesa ativa");

        fakes.save(table.deactivate());
        deactivate.deactivate(sakura, deck.id());
        deactivate.deactivate(sakura, deck.id()); // de novo: tudo bem
        assertThat(fakes.sectors.get(deck.id()).active()).isFalse();
    }

    @Test
    void sectorOfAnotherRestaurantIsNotFound() {
        Sector elsewhere = fakes.givenSector(EstablishmentId.newId(), "Deck");

        assertThatThrownBy(() -> rename.rename(sakura, elsewhere.id(), "Meu deck"))
                .isInstanceOf(SectorNotFoundException.class);
        assertThatThrownBy(() -> deactivate.deactivate(sakura, elsewhere.id()))
                .isInstanceOf(SectorNotFoundException.class);
    }
}
