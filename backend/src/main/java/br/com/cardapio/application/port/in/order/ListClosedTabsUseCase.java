package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabPeriod;

public interface ListClosedTabsUseCase {

    /**
     * O histórico: as comandas fechadas nestes dias (no fuso do restaurante), da mais nova à mais
     * antiga.
     *
     * @param tableId só desta mesa; null = todas
     */
    ResultPage<ClosedTab> list(EstablishmentId establishmentId, TabPeriod period, DiningTableId tableId,
                               PageQuery page);
}
