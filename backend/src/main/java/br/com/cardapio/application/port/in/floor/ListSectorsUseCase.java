package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

public interface ListSectorsUseCase {

    ResultPage<SectorSummary> list(EstablishmentId establishmentId, PageQuery page);
}
