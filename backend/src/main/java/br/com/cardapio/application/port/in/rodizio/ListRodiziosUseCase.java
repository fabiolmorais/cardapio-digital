package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;

public interface ListRodiziosUseCase {

    /** Todos os rodízios do cadastro, os ativos primeiro e depois os pausados, por nome. */
    ResultPage<Rodizio> list(EstablishmentId establishmentId, PageQuery page);

    Rodizio get(EstablishmentId establishmentId, RodizioId id);
}
