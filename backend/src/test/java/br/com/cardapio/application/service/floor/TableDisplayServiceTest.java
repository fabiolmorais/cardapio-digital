package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.DisplayFormat;
import br.com.cardapio.application.port.in.floor.RenderedFile;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.floor.DisplayContent;
import br.com.cardapio.application.port.out.floor.TableDisplayRenderer;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.BrandColor;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.brand.model.Theme;
import br.com.cardapio.domain.floor.exception.NothingToPrintException;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.support.InMemoryStorage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class TableDisplayServiceTest {

    /** Renderizador falso: guarda o que pediram e devolve o pedido como "arquivo". */
    static final class RecordingRenderer implements TableDisplayRenderer {
        final List<String> urls = new ArrayList<>();
        final List<List<DisplayContent>> pdfs = new ArrayList<>();

        @Override
        public byte[] qrPng(String url) {
            urls.add(url);
            return ("png:" + url).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] qrSvg(String url) {
            urls.add(url);
            return ("svg:" + url).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] displaysPdf(List<DisplayContent> displays) {
            pdfs.add(displays);
            return "pdf".getBytes(StandardCharsets.UTF_8);
        }
    }

    private final FloorFakes floor = new FloorFakes();
    private final RecordingRenderer renderer = new RecordingRenderer();
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final InMemoryStorage storage = new InMemoryStorage();
    private Brand brand = new Brand(sakura, "Sakura Sushi", BrandColor.PETROL, Theme.DARK, null, null, null);
    private final BrandPort brands = new BrandPort() {
        @Override
        public Brand find(EstablishmentId establishmentId) {
            return brand;
        }

        @Override
        public void save(Brand changed) {
            brand = changed;
        }
    };
    // A barra no fim é de propósito: o serviço não pode gerar "//m/".
    private final TableDisplayService service = new TableDisplayService(floor, floor, brands, storage, renderer,
            "https://app.cardapio.com.br/");

    @Test
    void pngAndSvgCarryTheTableUrlAndAGoodFileName() {
        Sector salao = floor.givenSector(sakura, "Salão");
        DiningTable table = floor.givenTable(salao, "Varanda 2");

        RenderedFile png = service.render(sakura, table.id(), DisplayFormat.PNG);
        RenderedFile svg = service.render(sakura, table.id(), DisplayFormat.SVG);

        String url = "https://app.cardapio.com.br/m/" + table.token().value();
        assertThat(renderer.urls).containsExactly(url, url);
        assertThat(png.fileName()).isEqualTo("mesa-varanda-2.png");
        assertThat(png.contentType()).isEqualTo("image/png");
        assertThat(svg.fileName()).isEqualTo("mesa-varanda-2.svg");
    }

    @Test
    void pdfOfATableHasTheRestaurantAndTheLabel() {
        DiningTable table = floor.givenTable(floor.givenSector(sakura, "Salão"), "04");

        RenderedFile pdf = service.render(sakura, table.id(), DisplayFormat.PDF);

        assertThat(pdf.fileName()).isEqualTo("mesa-04.pdf");
        assertThat(renderer.pdfs.getFirst())
                .extracting(DisplayContent::establishmentName, DisplayContent::tableLabel, DisplayContent::accentColor,
                        DisplayContent::logoPng)
                .containsExactly(tuple("Sakura Sushi", "04", BrandColor.PETROL.textOnLight(), null));
    }

    @Test
    void thePdfCarriesTheSmallLogoButThePngDoesNotNeedIt() {
        DiningTable table = floor.givenTable(floor.givenSector(sakura, "Salão"), "04");
        brand = brand.withNewLogo();
        byte[] logo = "logo pequena".getBytes(StandardCharsets.UTF_8);
        storage.store(brand.logoFile(LogoVariant.SMALL), logo, "image/png");

        service.render(sakura, table.id(), DisplayFormat.PDF);
        assertThat(renderer.pdfs.getFirst().getFirst().logoPng()).isEqualTo(logo);

        // A logo sumiu do armazenamento: o display sai sem ela, em vez de falhar.
        storage.files.clear();
        service.render(sakura, table.id(), DisplayFormat.PDF);
        assertThat(renderer.pdfs.get(1).getFirst().logoPng()).isNull();
    }

    @Test
    void sectorPdfHasOnlyItsActiveTables() {
        Sector deck = floor.givenSector(sakura, "Área externa");
        floor.givenTable(deck, "D1");
        floor.save(floor.givenTable(deck, "D2").deactivate());
        floor.givenTable(floor.givenSector(sakura, "Salão"), "01");

        RenderedFile pdf = service.render(sakura, deck.id());

        assertThat(pdf.fileName()).isEqualTo("setor-area-externa.pdf");
        assertThat(renderer.pdfs.getFirst()).extracting(DisplayContent::tableLabel).containsExactly("D1");
    }

    @Test
    void sectorWithoutActiveTablesHasNothingToPrint() {
        Sector empty = floor.givenSector(sakura, "Deck");

        assertThatThrownBy(() -> service.render(sakura, empty.id())).isInstanceOf(NothingToPrintException.class);
    }

    @Test
    void anotherRestaurantsTableOrSectorIsNotFound() {
        Sector theirs = floor.givenSector(EstablishmentId.newId(), "Salão deles");
        DiningTable table = floor.givenTable(theirs, "01");

        assertThatThrownBy(() -> service.render(sakura, table.id(), DisplayFormat.PDF))
                .isInstanceOf(TableNotFoundException.class);
        assertThatThrownBy(() -> service.render(sakura, theirs.id())).isInstanceOf(SectorNotFoundException.class);
        assertThat(renderer.urls).isEmpty();
    }

    @Test
    void fileNamesHaveNoAccentsNorSpaces() {
        assertThat(TableDisplayService.fileSafe("Área 1")).isEqualTo("area-1");
        assertThat(TableDisplayService.fileSafe("  B-12 ")).isEqualTo("b-12");
        assertThat(TableDisplayService.fileSafe("—")).isEqualTo("mesa");
    }
}
