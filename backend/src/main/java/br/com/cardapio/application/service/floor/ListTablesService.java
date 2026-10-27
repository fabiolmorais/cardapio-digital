package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.ListTablesUseCase;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import org.springframework.transaction.annotation.Transactional;

public class ListTablesService implements ListTablesUseCase {

    private final DiningTablePort tables;

    public ListTablesService(DiningTablePort tables) {
        this.tables = tables;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<TableSummary> list(EstablishmentId establishmentId, SectorId sectorId, PageQuery page) {
        return tables.findAll(establishmentId, sectorId, page).map(TableSummary::from);
    }
}
