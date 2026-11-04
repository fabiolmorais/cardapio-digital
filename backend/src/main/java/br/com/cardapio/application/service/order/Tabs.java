package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.Tab;

import java.time.Instant;

/** O que os casos de uso da comanda repetem. */
public final class Tabs {

    private Tabs() {
    }

    /**
     * A comanda aberta da mesa, ou uma nova: ela abre no primeiro pedido (especificação) ou na
     * ativação do rodízio. Se dois pedidos chegarem no mesmo instante, o índice único deixa abrir
     * só uma, e o outro usa ela.
     */
    public static Tab openOrCreate(TabPort tabs, EstablishmentId establishmentId, DiningTableId tableId,
                                   Instant now) {
        return tabs.findOpen(establishmentId, tableId).orElseGet(() -> {
            Tab tab = Tab.open(establishmentId, tableId, now);
            return tabs.insertIfNoneOpen(tab) ? tab : tabs.findOpen(establishmentId, tableId).orElseThrow();
        });
    }
}
