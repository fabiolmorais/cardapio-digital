package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;

public interface GetStaffOrderSheetUseCase {

    /** A situação da mesa para lançar um pedido. TableNotFoundException se não for deste restaurante. */
    StaffOrderSheet get(EstablishmentId establishmentId, DiningTableId tableId);
}
