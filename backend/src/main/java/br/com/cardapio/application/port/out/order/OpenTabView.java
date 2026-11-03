package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.shared.model.Money;

/**
 * Uma comanda aberta com o resumo que o painel mostra: a mesa, o setor, o total até agora e
 * quantos itens ainda estão a caminho. Calculado no banco, numa consulta para a página inteira.
 */
public record OpenTabView(Tab tab, String tableLabel, String sectorName, Money total, int itemsOnTheWay) {
}
