package br.com.cardapio.application.port.in.order;

public interface MoveTabUseCase {

    /**
     * Muda a comanda de mesa, ou junta com a comanda da mesa de destino. Vão junto os pedidos, os
     * pagamentos, o rodízio, os chamados abertos e os celulares (ADR 0012).
     */
    MovedTab move(MoveTabCommand command);
}
