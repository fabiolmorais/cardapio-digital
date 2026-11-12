package br.com.cardapio.application.port.in.live;

import br.com.cardapio.application.port.in.order.KitchenItem;

import java.util.List;

/**
 * O que a TV vinculada mostra além dos chamados (desenho 8.1): os itens na cozinha e prontos, os
 * setores do salão e a espera média de hoje.
 *
 * @param averageWaitSeconds a espera média dos chamados de hoje; null se ninguém chamou
 */
public record TvBoard(List<KitchenItem> kitchen, List<String> sectors, Long averageWaitSeconds) {

    public TvBoard {
        kitchen = List.copyOf(kitchen);
        sectors = List.copyOf(sectors);
    }
}
