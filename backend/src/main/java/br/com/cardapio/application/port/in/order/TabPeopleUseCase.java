package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeople;

import java.util.Optional;

/** Quantas pessoas estão na mesa (fora do rodízio), informado pelo garçom: opcional. */
public interface TabPeopleUseCase {

    Optional<TabPeople> get(EstablishmentId establishmentId, TabId tabId);

    /** Só na comanda aberta; null apaga. */
    Optional<TabPeople> set(EstablishmentId establishmentId, TabId tabId, TabPeople people);
}
