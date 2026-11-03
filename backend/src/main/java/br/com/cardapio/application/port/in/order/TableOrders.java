package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.shared.model.Money;

import java.util.List;

/**
 * Os pedidos da comanda aberta da mesa.
 *
 * @param orderingEnabled o restaurante aceita pedido pelo celular (a tela esconde o carrinho se não)
 * @param total           a soma dos itens não cancelados (o total parcial da especificação)
 */
public record TableOrders(boolean orderingEnabled, Money total, List<TableOrder> orders) {

    public TableOrders {
        orders = List.copyOf(orders);
    }
}
