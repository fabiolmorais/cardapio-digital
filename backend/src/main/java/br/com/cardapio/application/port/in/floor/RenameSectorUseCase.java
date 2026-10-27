package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;

public interface RenameSectorUseCase {

    SectorSummary rename(EstablishmentId establishmentId, SectorId sectorId, String newName);
}
