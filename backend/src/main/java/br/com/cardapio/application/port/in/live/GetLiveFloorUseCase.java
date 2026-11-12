package br.com.cardapio.application.port.in.live;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface GetLiveFloorUseCase {

    /** @param withRevenue mostrar o faturado de hoje (só o dono e o gerente veem) */
    LiveFloor get(EstablishmentId establishmentId, boolean withRevenue);
}
