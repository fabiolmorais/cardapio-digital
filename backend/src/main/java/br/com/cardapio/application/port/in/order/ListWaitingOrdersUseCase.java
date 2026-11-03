package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

public interface ListWaitingOrdersUseCase {

    /** Os pedidos parados na confirmação do garçom, do mais antigo ao mais novo. */
    ResultPage<WaitingOrder> list(EstablishmentId establishmentId, PageQuery page);
}
