package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.ListWaitingOrdersUseCase;
import br.com.cardapio.application.port.in.order.WaitingOrder;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListWaitingOrdersService implements ListWaitingOrdersUseCase {

    private final OrderPort orders;

    public ListWaitingOrdersService(OrderPort orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<WaitingOrder> list(EstablishmentId establishmentId, PageQuery page) {
        return orders.findWaitingApproval(establishmentId, page).map(WaitingOrder::from);
    }
}
