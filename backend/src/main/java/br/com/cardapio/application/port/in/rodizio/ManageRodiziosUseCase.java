package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;

/** O cadastro dos rodízios (dono e gerente). */
public interface ManageRodiziosUseCase {

    Rodizio create(EstablishmentId establishmentId, RodizioForm form);

    Rodizio update(EstablishmentId establishmentId, RodizioId id, RodizioForm form);

    /** Tira o rodízio da lista. As mesas que já estão nele continuam até a conta (Guia 25). */
    void deactivate(EstablishmentId establishmentId, RodizioId id);
}
