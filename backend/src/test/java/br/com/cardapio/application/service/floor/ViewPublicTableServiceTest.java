package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.PublicTable;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.InstagramHandle;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.brand.model.Theme;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.TableToken;
import br.com.cardapio.support.InMemoryStorage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViewPublicTableServiceTest {

    private final FloorFakes floor = new FloorFakes();
    private Brand brand;
    /** Toda mesa é da "Sakura Sushi": este teste é sobre o token e a marca, não sobre vários restaurantes. */
    private final BrandPort brands = new BrandPort() {
        @Override
        public Brand find(EstablishmentId establishmentId) {
            return new Brand(establishmentId, brand.name(), brand.color(), brand.theme(), brand.logoPath(),
                    brand.instagram(), brand.reviewLink());
        }

        @Override
        public void save(Brand changed) {
            brand = changed;
        }
    };
    private final ViewPublicTableService service = new ViewPublicTableService(floor, floor, brands,
            new InMemoryStorage());

    {
        brand = new Brand(EstablishmentId.newId(), "Sakura Sushi", BrandColor.PETROL, Theme.LIGHT, null,
                new InstagramHandle("sakurasushi.sp"), null);
    }

    @Test
    void tokenOpensItsTableWithTheSectorAndTheBrand() {
        DiningTable table = floor.givenTable(floor.givenSector(EstablishmentId.newId(), "Salão principal"), "04");

        PublicTable found = service.view(table.token().value());

        assertThat(found.tableLabel()).isEqualTo("04");
        assertThat(found.sectorName()).isEqualTo("Salão principal");
        assertThat(found.establishmentName()).isEqualTo("Sakura Sushi");
        assertThat(found.color()).isEqualTo(BrandColor.PETROL);
        assertThat(found.theme()).isEqualTo(Theme.LIGHT);
        assertThat(found.instagram()).isEqualTo("sakurasushi.sp");
        assertThat(found.logoUrls()).isEmpty();

        brand = brand.withNewLogo();
        assertThat(service.view(table.token().value()).logoUrls())
                .containsEntry(LogoVariant.SMALL, "https://fotos.test/" + brand.logoFile(LogoVariant.SMALL));
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
