package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ClosedTab;
import br.com.cardapio.application.port.in.order.ListClosedTabsUseCase;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabPeriod;
import org.springframework.transaction.annotation.Transactional;

public class ListClosedTabsService implements ListClosedTabsUseCase {

    private final TabPort tabs;

    public ListClosedTabsService(TabPort tabs) {
        this.tabs = tabs;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<ClosedTab> list(EstablishmentId establishmentId, TabPeriod period, DiningTableId tableId,
                                      PageQuery page) {
        return tabs.findClosed(establishmentId, period, tableId, page).map(ClosedTab::from);
    }
}
