package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.order.model.Tab;

/** A comanda com a mesa e o setor (para a equipe saber de onde é). */
public record TabView(Tab tab, String tableLabel, String sectorName) {
}
