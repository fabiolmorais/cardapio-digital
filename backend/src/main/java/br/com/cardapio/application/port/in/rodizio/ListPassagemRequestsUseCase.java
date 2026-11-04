package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.application.port.in.order.KitchenItem;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

/** Os cortes pedidos pelas mesas do rodízio de passagem, para os passadores levarem. */
public interface ListPassagemRequestsUseCase {

    ResultPage<KitchenItem> list(EstablishmentId establishmentId, PageQuery page);
}
