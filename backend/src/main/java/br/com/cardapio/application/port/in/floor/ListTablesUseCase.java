package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;

public interface ListTablesUseCase {

    /** sectorId vazio: todas as mesas do restaurante. */
    ResultPage<TableSummary> list(EstablishmentId establishmentId, SectorId sectorId, PageQuery page);
}
