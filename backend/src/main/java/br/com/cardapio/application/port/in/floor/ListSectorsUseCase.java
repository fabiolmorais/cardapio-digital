package br.com.cardapio.application.port.in.floor;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

public interface ListSectorsUseCase {

    /** Só os ativos (as listas de escolha: mover mesa, filtros). */
    default ResultPage<SectorSummary> list(EstablishmentId establishmentId, PageQuery page) {
        return list(establishmentId, false, page);
    }

    /** Com includeInactive, também os desativados, depois dos ativos (a tela de mesas, para reativar). */
    ResultPage<SectorSummary> list(EstablishmentId establishmentId, boolean includeInactive, PageQuery page);
}
