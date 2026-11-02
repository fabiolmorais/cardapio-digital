package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.DisplayFormat;
import br.com.cardapio.application.port.in.floor.RenderSectorDisplaysUseCase;
import br.com.cardapio.application.port.in.floor.RenderTableDisplayUseCase;
import br.com.cardapio.application.port.in.floor.RenderedFile;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.DisplayContent;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.floor.TableDisplayRenderer;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.floor.exception.NothingToPrintException;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Os arquivos para impressão. Uma classe só atende aos dois casos de uso (uma mesa e o setor
 * inteiro): eles dividem as mesmas dependências e a mesma montagem do conteúdo.
 */
public class TableDisplayService implements RenderTableDisplayUseCase, RenderSectorDisplaysUseCase {

    /** Um setor tem no máximo isto de mesas numa página de busca; os planos vão até 100 mesas. */
    private static final PageQuery ALL_TABLES = new PageQuery(0, PageQuery.MAX_SIZE);
    /** A logo pequena (128 px) que nós mesmos geramos tem bem menos que isto. */
    private static final long LOGO_MAX_BYTES = 512L * 1024;

    private final DiningTablePort tables;
    private final SectorPort sectors;
    private final BrandPort brands;
    private final MediaStoragePort storage;
    private final TableDisplayRenderer renderer;
    private final String publicUrl;

    public TableDisplayService(DiningTablePort tables, SectorPort sectors, BrandPort brands, MediaStoragePort storage,
                               TableDisplayRenderer renderer, String publicUrl) {
        this.tables = tables;
        this.sectors = sectors;
        this.brands = brands;
        this.storage = storage;
        this.renderer = renderer;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public RenderedFile render(EstablishmentId establishmentId, DiningTableId tableId, DisplayFormat format) {
        DiningTable table = tables.find(establishmentId, tableId).orElseThrow(TableNotFoundException::new);
        DisplayContent content = content(printedBrand(establishmentId, format == DisplayFormat.PDF), table);
        byte[] bytes = switch (format) {
            case PNG -> renderer.qrPng(content.url());
            case SVG -> renderer.qrSvg(content.url());
            case PDF -> renderer.displaysPdf(List.of(content));
        };
        return new RenderedFile("mesa-" + fileSafe(table.label()) + "." + format.extension(), format.contentType(), bytes);
    }

    @Override
    @Transactional(readOnly = true)
    public RenderedFile render(EstablishmentId establishmentId, SectorId sectorId) {
        Sector sector = sectors.find(establishmentId, sectorId)
                .filter(Sector::active)
                .orElseThrow(SectorNotFoundException::new);
        PrintedBrand brand = printedBrand(establishmentId, true);
        List<DisplayContent> displays = tables.findAll(establishmentId, sectorId, ALL_TABLES).content().stream()
                .filter(DiningTable::active)
                .map(table -> content(brand, table))
                .toList();
        if (displays.isEmpty()) {
            throw new NothingToPrintException();
        }
        return new RenderedFile("setor-" + fileSafe(sector.name()) + ".pdf", DisplayFormat.PDF.contentType(),
                renderer.displaysPdf(displays));
    }

    /** A marca como vai impressa: nome, cor para fundo branco e a logo pequena (lida uma vez por arquivo). */
    private record PrintedBrand(String name, String accentColor, byte[] logoPng) {
    }

    /**
     * withLogo: só o PDF usa a logo (o PNG e o SVG são só o QR), e lê-la do armazenamento custa uma
     * ida à rede. Se a logo sumiu do armazenamento, o display sai com o nome, sem logo, em vez de falhar.
     */
    private PrintedBrand printedBrand(EstablishmentId establishmentId, boolean withLogo) {
        Brand brand = brands.find(establishmentId);
        byte[] logo = withLogo && brand.hasLogo()
                ? storage.read(brand.logoFile(LogoVariant.SMALL), LOGO_MAX_BYTES).orElse(null)
                : null;
        return new PrintedBrand(brand.name(), brand.color().textOnLight(), logo);
    }

    private DisplayContent content(PrintedBrand brand, DiningTable table) {
        return new DisplayContent(brand.name(), table.label(), publicUrl + "/m/" + table.token().value(),
                brand.accentColor(), brand.logoPng());
    }

    /** "Varanda 2" → "varanda-2", "Área 1" → "area-1": nome de arquivo sem acento nem espaço. */
    static String fileSafe(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String safe = plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return safe.isEmpty() ? "mesa" : safe;
    }
}
