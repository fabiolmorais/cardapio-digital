package br.com.cardapio.application.port.in.order;

public interface PlaceStaffOrderUseCase {

    /**
     * Lança o pedido na comanda da mesa, abrindo-a se preciso. DuplicateOrderException se a mesa já
     * pediu algum destes produtos e quem lançou ainda não confirmou.
     */
    StaffOrder place(PlaceStaffOrderCommand command);
}
