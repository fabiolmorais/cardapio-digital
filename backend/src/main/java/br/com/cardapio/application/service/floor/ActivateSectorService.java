package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.ActivateSectorUseCase;
import br.com.cardapio.application.port.in.floor.SectorSummary;
import br.com.cardapio.application.port.out.floor.SectorOverview;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorNameTakenException;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;
import org.springframework.transaction.annotation.Transactional;

public class ActivateSectorService implements ActivateSectorUseCase {

    private final SectorPort sectors;

    public ActivateSectorService(SectorPort sectors) {
        this.sectors = sectors;
    }

    @Override
    @Transactional
    public SectorSummary activate(EstablishmentId establishmentId, SectorId sectorId) {
        Sector sector = sectors.find(establishmentId, sectorId).orElseThrow(SectorNotFoundException::new);
        if (!sector.active()) {
            // Enquanto esteve desativado, outro setor pode ter ficado com o nome.
            if (sectors.activeNameExists(establishmentId, sector.name(), sectorId)) {
                throw new SectorNameTakenException(sector.name());
            }
            sector = sector.activate();
            sectors.save(sector);
        }
        return SectorSummary.from(new SectorOverview(sector, sectors.countActiveTables(establishmentId, sectorId)));
    }
}
