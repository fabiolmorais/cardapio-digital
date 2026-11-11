package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;

public interface ActivateSectorUseCase {

    /**
     * "Reativar setor": volta para a lista, e as mesas dele podem ser reativadas. Reativar de novo
     * não é erro. SectorNameTakenException se já existir um setor ativo com o mesmo nome.
     */
    SectorSummary activate(EstablishmentId establishmentId, SectorId sectorId);
}
