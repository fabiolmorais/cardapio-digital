package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.out.order.OpenTabView;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;

/** Uma comanda aberta no painel: a mesa, desde quando, quanto dá e se ainda falta entregar algo. */
public record OpenTab(TabId id, DiningTableId tableId, String tableLabel, String sectorName, Instant openedAt,
                      Money total, int itemsOnTheWay) {

    public static OpenTab from(OpenTabView view) {
        var tab = view.tab();
        return new OpenTab(tab.id(), tab.tableId(), view.tableLabel(), view.sectorName(), tab.openedAt(),
                view.total(), view.itemsOnTheWay());
    }
}
