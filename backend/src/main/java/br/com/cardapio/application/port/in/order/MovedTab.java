package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;

/**
 * Depois da mudança: a comanda que continua (na junção, a da mesa de destino) e a mesa dela.
 *
 * @param merged as duas comandas se juntaram
 */
public record MovedTab(TabId tabId, DiningTableId tableId, boolean merged) {
}
