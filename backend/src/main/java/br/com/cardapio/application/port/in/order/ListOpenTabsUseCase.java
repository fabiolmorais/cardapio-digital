package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

public interface ListOpenTabsUseCase {

    /** As comandas abertas, da mais antiga à mais nova. */
    ResultPage<OpenTab> list(EstablishmentId establishmentId, PageQuery page);
}
