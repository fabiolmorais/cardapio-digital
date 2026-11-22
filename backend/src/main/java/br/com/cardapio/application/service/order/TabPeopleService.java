package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.TabPeopleUseCase;
import br.com.cardapio.application.port.out.order.TabPeoplePort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeople;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Quantas pessoas estão na mesa fora do rodízio (REL-04), informado pelo garçom. */
public class TabPeopleService implements TabPeopleUseCase {

    private final TabPort tabs;
    private final TabPeoplePort people;

    public TabPeopleService(TabPort tabs, TabPeoplePort people) {
        this.tabs = tabs;
        this.people = people;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TabPeople> get(EstablishmentId establishmentId, TabId tabId) {
        tabs.findView(establishmentId, tabId).orElseThrow(TabNotFoundException::new);
        return people.find(establishmentId, tabId);
    }

    @Override
    @Transactional
    public Optional<TabPeople> set(EstablishmentId establishmentId, TabId tabId, TabPeople count) {
        Tab tab = tabs.findView(establishmentId, tabId).orElseThrow(TabNotFoundException::new).tab();
        if (!tab.status().isOpen()) {
            throw new TabStateException("Esta comanda já foi fechada");
        }
        people.save(establishmentId, tabId, count);
        return Optional.ofNullable(count);
    }
}
