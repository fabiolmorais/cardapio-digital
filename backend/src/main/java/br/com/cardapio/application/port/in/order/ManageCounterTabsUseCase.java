package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;

/** As comandas do balcão (RF45, ADR 0015): abrir, corrigir o nome e listar as abertas de um balcão. */
public interface ManageCounterTabsUseCase {

    OpenTab open(OpenCounterTabCommand command);

    OpenTab rename(RenameCounterTabCommand command);

    /** As comandas abertas deste balcão, da mais antiga para a mais nova. */
    ResultPage<OpenTab> list(EstablishmentId establishmentId, DiningTableId counterId, PageQuery page);
}
