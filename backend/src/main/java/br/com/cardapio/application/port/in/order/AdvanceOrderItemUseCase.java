package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.OrderItemId;

public interface AdvanceOrderItemUseCase {

    /** Em preparo, pronto ou entregue. */
    void advance(EstablishmentId establishmentId, OrderItemId itemId, OrderItemStep step, UserId by);
}
