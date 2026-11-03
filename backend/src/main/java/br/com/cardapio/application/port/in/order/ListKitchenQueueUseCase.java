package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.OrderItemStatus;

import java.util.Set;

public interface ListKitchenQueueUseCase {

    /**
     * Os itens a caminho dos pedidos que foram para a cozinha, do mais antigo ao mais novo.
     *
     * @param statuses só estes (na fila, em preparo, pronto); vazio = os três
     */
    ResultPage<KitchenItem> list(EstablishmentId establishmentId, Set<OrderItemStatus> statuses, PageQuery page);
}
