package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;

public interface DeactivateSectorUseCase {

    /** Só desativa setor sem mesas ativas (SectorNotEmptyException). Desativar de novo não é erro. */
    void deactivate(EstablishmentId establishmentId, SectorId sectorId);
}
