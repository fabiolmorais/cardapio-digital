package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ListOpenTabsUseCase;
import br.com.cardapio.application.port.in.order.OpenTab;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListOpenTabsService implements ListOpenTabsUseCase {

    private final TabPort tabs;

    public ListOpenTabsService(TabPort tabs) {
        this.tabs = tabs;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<OpenTab> list(EstablishmentId establishmentId, PageQuery page) {
        return tabs.findOpenTabs(establishmentId, page).map(OpenTab::from);
    }
}
