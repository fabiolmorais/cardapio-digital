package br.com.cardapio.application.port.in.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;

import java.util.Set;

/** O cadastro dos rodízios (dono e gerente). */
public interface ManageRodiziosUseCase {

    Rodizio create(EstablishmentId establishmentId, RodizioForm form);

    Rodizio update(EstablishmentId establishmentId, RodizioId id, RodizioForm form);

    /**
     * Pausa o rodízio: as mesas não podem mais pedir, mas ele fica no cadastro (para reativar). As
     * mesas que já estão nele continuam até a conta (Guia 25).
     */
    void deactivate(EstablishmentId establishmentId, RodizioId id);

    /** Volta a oferecer o rodízio pausado (com as mesmas regras de nome e de limite da criação). */
    Rodizio reactivate(EstablishmentId establishmentId, RodizioId id);

    /**
     * Em quais rodízios (ativos ou pausados) o produto está: entra nos escolhidos e sai dos outros,
     * numa transação só, sem mexer no resto de cada rodízio. Devolve os rodízios em que ele ficou.
     */
    Set<RodizioId> setProductRodizios(EstablishmentId establishmentId, ProductId productId, Set<RodizioId> rodizioIds);
}
