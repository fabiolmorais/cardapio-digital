package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;

public interface GetTabUseCase {

    /** Uma comanda com os pedidos (aberta ou fechada). */
    StaffTab get(EstablishmentId establishmentId, TabId tabId);
}
