package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.CreateTableCommand;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.in.floor.UpdateTableCommand;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.exception.TableLabelTakenException;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.Sector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableServicesTest {

    private final FloorFakes fakes = new FloorFakes();
    private final CreateTableService create = new CreateTableService(fakes, fakes, fakes, fakes.clock());
    private final UpdateTableService update = new UpdateTableService(fakes, fakes);
    private final RotateTableTokenService rotate = new RotateTableTokenService(fakes, fakes, fakes.clock());
    private final ListTablesService list = new ListTablesService(fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private Sector salao;
    private Sector deck;

    @BeforeEach
    void twoSectors() {
        salao = fakes.givenSector(sakura, "Salão");
        deck = fakes.givenSector(sakura, "Deck");
    }

    private TableSummary createTable(Sector sector, String label) {
        return create.create(new CreateTableCommand(sakura, sector.id(), label));
    }

    @Test
    void newTableComesWithItsOwnToken() {
        TableSummary t04 = createTable(salao, "04");
        TableSummary t05 = createTable(salao, "05");

        assertThat(t04.publicToken()).isNotEqualTo(t05.publicToken());
        assertThat(t04.tokenIssuedAt()).isEqualTo(FloorFakes.NOW);
        assertThat(t04.active()).isTrue();
    }

    @Test
    void labelsAreUniqueInTheRestaurantEvenAcrossSectors() {
        createTable(salao, "B-1");

        assertThatThrownBy(() -> createTable(deck, "b-1")).isInstanceOf(TableLabelTakenException.class);
    }

    @Test
    void listsTwoBeforeTenAndCanFilterBySector() {
        createTable(salao, "10");
        createTable(salao, "2");
        createTable(deck, "1");

        assertThat(list.list(sakura, null, new PageQuery(0, 20)).content())
                .extracting(TableSummary::label).containsExactly("1", "2", "10");
        assertThat(list.list(sakura, salao.id(), new PageQuery(0, 20)).content())
                .extracting(TableSummary::label).containsExactly("2", "10");
    }

    @Test
    void updateRenamesMovesAndDeactivates() {
        TableSummary table = createTable(salao, "04");

        TableSummary changed = update.update(new UpdateTableCommand(sakura, table.id(), "D4", deck.id(), false));

        assertThat(changed.label()).isEqualTo("D4");
        assertThat(changed.sectorId()).isEqualTo(deck.id());
        assertThat(changed.active()).isFalse();
        assertThat(changed.publicToken()).isEqualTo(table.publicToken()); // o QR não muda
    }

    @Test
    void cannotMoveToAnotherRestaurantsSectorNorToAnInactiveOne() {
        TableSummary table = createTable(salao, "04");
        Sector elsewhere = fakes.givenSector(EstablishmentId.newId(), "Deles");
        fakes.save(deck.deactivate());

        assertThatThrownBy(() -> update.update(new UpdateTableCommand(sakura, table.id(), null, elsewhere.id(), null)))
                .isInstanceOf(SectorNotFoundException.class);
        assertThatThrownBy(() -> update.update(new UpdateTableCommand(sakura, table.id(), null, deck.id(), null)))
                .isInstanceOf(SectorNotFoundException.class);
        assertThatThrownBy(() -> createTable(elsewhere, "99")).isInstanceOf(SectorNotFoundException.class);
    }

    @Test
    void rotatingTheTokenInvalidatesTheOldQr() {
        TableSummary table = createTable(salao, "04");

        TableSummary rotated = rotate.rotate(sakura, table.id());

        assertThat(rotated.publicToken()).isNotEqualTo(table.publicToken());
        DiningTable stored = fakes.tables.get(table.id());
        assertThat(stored.token().value()).isEqualTo(rotated.publicToken());
    }

    @Test
    void tableOfAnotherRestaurantIsNotFound() {
        Sector theirs = fakes.givenSector(EstablishmentId.newId(), "Salão deles");
        DiningTable elsewhere = fakes.givenTable(theirs, "01");

        assertThatThrownBy(() -> rotate.rotate(sakura, elsewhere.id())).isInstanceOf(TableNotFoundException.class);
        assertThatThrownBy(() -> update.update(new UpdateTableCommand(sakura, elsewhere.id(), "X", null, null)))
                .isInstanceOf(TableNotFoundException.class);
    }
}
