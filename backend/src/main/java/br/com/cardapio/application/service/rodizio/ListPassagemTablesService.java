package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.ListPassagemTablesUseCase;
import br.com.cardapio.application.port.in.rodizio.PassagemTable;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListPassagemTablesService implements ListPassagemTablesUseCase {

    private final TabRodizioPort tabRodizios;

    public ListPassagemTablesService(TabRodizioPort tabRodizios) {
        this.tabRodizios = tabRodizios;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<PassagemTable> list(EstablishmentId establishmentId, PageQuery page) {
        return tabRodizios.findOpenPassagem(establishmentId, page).map(PassagemTable::from);
    }
}
