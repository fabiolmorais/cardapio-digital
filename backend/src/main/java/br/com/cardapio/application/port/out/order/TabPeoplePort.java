package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeople;

import java.util.Optional;

/** Quantas pessoas estão na mesa fora do rodízio (REL-04). */
public interface TabPeoplePort {

    Optional<TabPeople> find(EstablishmentId establishmentId, TabId tabId);

    /** null apaga (o garçom não sabe). */
    void save(EstablishmentId establishmentId, TabId tabId, TabPeople people);
}
