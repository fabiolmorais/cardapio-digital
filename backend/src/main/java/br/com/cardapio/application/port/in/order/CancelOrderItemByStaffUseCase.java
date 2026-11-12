package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.OrderItemId;

public interface CancelOrderItemByStaffUseCase {

    /**
     * A equipe cancela um item ainda não entregue, com o motivo que o cliente vê.
     *
     * @param quantity quantos cancelar (a cozinha só tem para parte); null = o item todo
     */
    void cancel(EstablishmentId establishmentId, OrderItemId itemId, UserId by, String reason, Integer quantity);
}
