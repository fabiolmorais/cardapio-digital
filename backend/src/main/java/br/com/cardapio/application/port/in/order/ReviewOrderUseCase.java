package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.OrderId;

/** A confirmação do garçom: o pedido parado vai para a cozinha, ou é recusado. */
public interface ReviewOrderUseCase {

    void approve(EstablishmentId establishmentId, OrderId orderId, UserId by);

    void reject(EstablishmentId establishmentId, OrderId orderId, UserId by);
}
