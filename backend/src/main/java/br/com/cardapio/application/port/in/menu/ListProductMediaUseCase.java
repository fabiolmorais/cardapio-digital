package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MediaOwner;

public interface ListProductMediaUseCase {

    /** Todas as mídias ativas (no máximo 8), numa página só. */
    ResultPage<ProductMediaSummary> list(EstablishmentId establishmentId, MediaOwner owner);
}
