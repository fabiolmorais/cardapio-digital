package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.DeactivateSectorUseCase;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorNotEmptyException;
import br.com.cardapio.domain.floor.exception.SectorNotFoundException;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.floor.model.SectorId;
import org.springframework.transaction.annotation.Transactional;

public class DeactivateSectorService implements DeactivateSectorUseCase {

    private final SectorPort sectors;

    public DeactivateSectorService(SectorPort sectors) {
        this.sectors = sectors;
    }

    @Override
    @Transactional
    public void deactivate(EstablishmentId establishmentId, SectorId sectorId) {
        Sector sector = sectors.find(establishmentId, sectorId).orElseThrow(SectorNotFoundException::new);
        if (!sector.active()) {
            return;
        }
        long activeTables = sectors.countActiveTables(establishmentId, sectorId);
        if (activeTables > 0) {
            throw new SectorNotEmptyException(activeTables);
        }
        sectors.save(sector.deactivate());
    }
}
