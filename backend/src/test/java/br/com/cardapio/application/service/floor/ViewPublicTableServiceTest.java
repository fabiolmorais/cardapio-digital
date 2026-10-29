package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.PublicTable;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.TableToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViewPublicTableServiceTest {

    private final FloorFakes floor = new FloorFakes();
    private final ViewPublicTableService service = new ViewPublicTableService(floor);

    @Test
    void tokenOpensItsTable() {
        DiningTable table = floor.givenTable(floor.givenSector(EstablishmentId.newId(), "Salão"), "04");

        PublicTable found = service.view(table.token().value());

        assertThat(found.tableLabel()).isEqualTo("04");
        assertThat(found.establishmentName()).isEqualTo("Sakura Sushi");
    }

    @Test
    void rotatedInactiveInventedOrMalformedTokensAreNotFound() {
        DiningTable table = floor.givenTable(floor.givenSector(EstablishmentId.newId(), "Salão"), "04");
        String oldToken = table.token().value();
        floor.save(table.rotateToken(new TableToken("token-novo-da-mesa-000000"), FloorFakes.NOW));
        DiningTable inactive = floor.givenTable(floor.givenSector(EstablishmentId.newId(), "Deck"), "D1");
        floor.save(inactive.deactivate());

        assertThatThrownBy(() -> service.view(oldToken)).isInstanceOf(TableNotFoundException.class);
        assertThatThrownBy(() -> service.view(inactive.token().value())).isInstanceOf(TableNotFoundException.class);
        assertThatThrownBy(() -> service.view("token-que-ninguem-gerou-00")).isInstanceOf(TableNotFoundException.class);
        assertThatThrownBy(() -> service.view("../../etc")).isInstanceOf(TableNotFoundException.class);
        assertThatThrownBy(() -> service.view(null)).isInstanceOf(TableNotFoundException.class);
    }
}
