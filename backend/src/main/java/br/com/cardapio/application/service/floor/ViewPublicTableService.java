package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.PublicTable;
import br.com.cardapio.application.port.in.floor.ViewPublicTableUseCase;
import br.com.cardapio.application.port.out.brand.BrandPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.PublicTableView;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;
import br.com.cardapio.domain.brand.model.LogoVariant;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.Sector;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ViewPublicTableService implements ViewPublicTableUseCase {

    private final DiningTablePort tables;
    private final SectorPort sectors;
    private final BrandPort brands;
    private final MediaStoragePort storage;

    public ViewPublicTableService(DiningTablePort tables, SectorPort sectors, BrandPort brands,
                                  MediaStoragePort storage) {
        this.tables = tables;
        this.sectors = sectors;
        this.brands = brands;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicTable view(String token) {
        PublicTableView found = PublicTableLookup.resolve(tables, token);
        DiningTable table = found.table();
        EstablishmentId establishmentId = table.establishmentId();
        Brand brand = brands.find(establishmentId);
        // Mesa ativa está sempre num setor ativo (Guia 7); o setor só falta se algo estiver muito errado.
        String sectorName = sectors.find(establishmentId, table.sectorId()).map(Sector::name).orElse("");
        Map<LogoVariant, String> logo = brand.hasLogo()
                ? Arrays.stream(LogoVariant.values())
                        .collect(Collectors.toMap(Function.identity(), v -> storage.publicUrl(brand.logoFile(v))))
                : Map.of();
        return new PublicTable(brand.name(), table.label(), sectorName, brand.color(), brand.theme(), logo,
                brand.instagram() == null ? null : brand.instagram().value(),
                brand.reviewLink() == null ? null : brand.reviewLink().value());
    }
}
