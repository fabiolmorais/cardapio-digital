package br.com.cardapio.application.service.floor;

import br.com.cardapio.application.port.in.floor.ListTablesUseCase;
import br.com.cardapio.application.port.in.floor.TableSummary;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.floor.TableActivity;
import br.com.cardapio.application.port.out.floor.TableActivityPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.floor.model.TableSituation;
import org.springframework.transaction.annotation.Transactional;

public class ListTablesService implements ListTablesUseCase {

    private final DiningTablePort tables;
    private final TableActivityPort activity;

    public ListTablesService(DiningTablePort tables, TableActivityPort activity) {
        this.tables = tables;
        this.activity = activity;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<TableSummary> list(EstablishmentId establishmentId, SectorId sectorId, PageQuery page) {
        ResultPage<DiningTable> found = tables.findAll(establishmentId, sectorId, page);
        var now = activity.findActivity(establishmentId, found.content().stream().map(DiningTable::id).toList());
        return found.map(t -> {
            TableActivity a = now.getOrDefault(t.id(), TableActivity.NONE);
            return TableSummary.from(t, TableSituation.of(t.active(), a.tabOpen(), a.billRequested(), a.rodizio()));
        });
    }
}
