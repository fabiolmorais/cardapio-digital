package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.RenameSectorUseCase;
import br.com.cardapio.application.port.in.floor.SectorSummary;
import br.com.cardapio.application.port.out.floor.SectorOverview;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorNameTakenException;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;
import org.springframework.transaction.annotation.Transactional;

public class RenameSectorService implements RenameSectorUseCase {

    private final SectorPort sectors;

    public RenameSectorService(SectorPort sectors) {
        this.sectors = sectors;
    }

    @Override
    @Transactional
    public SectorSummary rename(EstablishmentId establishmentId, SectorId sectorId, String newName) {
        Sector renamed = sectors.find(establishmentId, sectorId)
                .filter(Sector::active)
                .orElseThrow(SectorNotFoundException::new)
                .rename(newName);
        if (sectors.activeNameExists(establishmentId, renamed.name(), sectorId)) {
            throw new SectorNameTakenException(renamed.name());
        }
        sectors.save(renamed);
        return SectorSummary.from(new SectorOverview(renamed, sectors.countActiveTables(establishmentId, sectorId)));
    }
}
