package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface CreateSectorUseCase {

    SectorSummary create(EstablishmentId establishmentId, String name);
}
