package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.service.RoundPolicy;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;
import java.util.Set;

/**
 * Os pedidos da comanda aberta da mesa.
 *
 * @param orderingEnabled o restaurante aceita pedido pelo celular (a tela esconde o carrinho se não)
 * @param total           o total parcial da especificação: os itens não cancelados mais o rodízio
 * @param paid            o que a mesa já pagou adiantado (a comanda continua aberta)
 * @param due             o que falta pagar: o total menos o que já foi pago
 * @param rodizio         o rodízio da mesa; null se ela não está no rodízio
 * @param rodizioProducts os produtos incluídos (a aba Rodízio); vazio fora do rodízio
 * @param rodizioLimits   as regras ligadas, em números para a tela; null fora do rodízio
 */
public record TableOrders(boolean orderingEnabled, Money total, Money paid, Money due, TableRodizio rodizio, Set<ProductId> rodizioProducts,
                          RoundPolicy.Limits rodizioLimits, List<TableOrder> orders) {

    public TableOrders {
        rodizioProducts = Set.copyOf(rodizioProducts);
        orders = List.copyOf(orders);
    }
}
