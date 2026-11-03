package br.com.cardapio.application.port.in.order;

public interface PlaceOrderUseCase {

    /** Envia o carrinho: abre a comanda da mesa se for o primeiro pedido, e devolve o pedido feito. */
    TableOrder place(PlaceOrderCommand command);
}
