package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.CreateSectorUseCase;
import br.com.cardapio.application.port.in.floor.SectorSummary;
import br.com.cardapio.application.port.out.floor.SectorOverview;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.SectorNameTakenException;
import br.com.cardapio.domain.floor.model.Sector;
import br.com.cardapio.domain.plan.model.PlanResource;
import org.springframework.transaction.annotation.Transactional;

public class CreateSectorService implements CreateSectorUseCase {

    private final SectorPort sectors;
    private final SubscriptionPort subscriptions;

    public CreateSectorService(SectorPort sectors, SubscriptionPort subscriptions) {
        this.sectors = sectors;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional
    public SectorSummary create(EstablishmentId establishmentId, String name) {
        // Cria primeiro (valida e limpa o nome) e só então confere se ele já existe.
        Sector sector = Sector.create(establishmentId, name, sectors.nextPosition(establishmentId));
        if (sectors.activeNameExists(establishmentId, sector.name(), null)) {
            throw new SectorNameTakenException(sector.name());
        }
        PlanAllowances.requireRoomForOneMore(subscriptions, establishmentId, PlanResource.SECTORS);
        sectors.save(sector);
        return SectorSummary.from(new SectorOverview(sector, 0));
    }
}
