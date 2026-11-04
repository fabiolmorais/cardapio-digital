package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.order.KitchenItem;
import br.com.cardapio.application.port.in.rodizio.ListPassagemRequestsUseCase;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListPassagemRequestsService implements ListPassagemRequestsUseCase {

    private final OrderPort orders;

    public ListPassagemRequestsService(OrderPort orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<KitchenItem> list(EstablishmentId establishmentId, PageQuery page) {
        return orders.findPassagemQueue(establishmentId, page).map(KitchenItem::from);
    }
}
