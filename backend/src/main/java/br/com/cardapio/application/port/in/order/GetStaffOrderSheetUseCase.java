package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;

public interface GetStaffOrderSheetUseCase {

    /** A situação da mesa para lançar um pedido. TableNotFoundException se não for deste restaurante. */
    StaffOrderSheet get(EstablishmentId establishmentId, DiningTableId tableId);

    /** A folha da comanda aberta de um cliente do balcão (ADR 0015): lançar está sempre liberado. */
    StaffOrderSheet getForCounterTab(EstablishmentId establishmentId, TabId tabId);
}
