package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

/** A tela dos passadores: as mesas abertas no rodízio de passagem, as pausadas primeiro. */
public interface ListPassagemTablesUseCase {

    ResultPage<PassagemTable> list(EstablishmentId establishmentId, PageQuery page);
}
