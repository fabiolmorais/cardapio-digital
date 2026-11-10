package br.com.cardapio.support;

import br.com.cardapio.application.port.out.rodizio.PassagemTableView;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.rodizio.model.TabRodizio;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Os rodízios do cadastro e os das comandas, na memória. */
public final class InMemoryRodizios implements RodizioPort, TabRodizioPort {

    public final Map<RodizioId, Rodizio> rodizios = new LinkedHashMap<>();
    public final Map<TabId, TabRodizio> byTab = new LinkedHashMap<>();

    // --- RodizioPort
    @Override
    public void save(Rodizio rodizio) { rodizios.put(rodizio.id(), rodizio); }

    @Override
    public Optional<Rodizio> find(EstablishmentId establishmentId, RodizioId id) {
        return Optional.ofNullable(rodizios.get(id)).filter(r -> r.establishmentId().equals(establishmentId));
    }

    @Override
    public ResultPage<Rodizio> findAll(EstablishmentId establishmentId, PageQuery page) {
        List<Rodizio> all = rodizios.values().stream()
                .filter(r -> r.establishmentId().equals(establishmentId))
                .sorted(Comparator.comparing((Rodizio r) -> !r.active()).thenComparing(Rodizio::name))
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public ResultPage<Rodizio> findActive(EstablishmentId establishmentId, PageQuery page) {
        List<Rodizio> all = rodizios.values().stream()
                .filter(r -> r.establishmentId().equals(establishmentId) && r.active())
                .sorted(Comparator.comparing(Rodizio::name))
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public long countActive(EstablishmentId establishmentId) {
        return findActive(establishmentId, new PageQuery(0, PageQuery.MAX_SIZE)).totalElements();
    }

    @Override
    public boolean activeNameExists(EstablishmentId establishmentId, String name, RodizioId exceptId) {
        return findActive(establishmentId, new PageQuery(0, PageQuery.MAX_SIZE)).content().stream()
                .anyMatch(r -> r.name().equalsIgnoreCase(name) && !r.id().equals(exceptId));
    }

    // --- TabRodizioPort
    @Override
    public boolean insertIfNone(TabRodizio tabRodizio) {
        return byTab.putIfAbsent(tabRodizio.tabId(), tabRodizio) == null;
    }

    @Override
    public Optional<TabRodizio> findByTab(EstablishmentId establishmentId, TabId tabId) {
        return Optional.ofNullable(byTab.get(tabId)).filter(r -> r.establishmentId().equals(establishmentId));
    }

    @Override
    public void updateGuests(TabRodizio tabRodizio) {
        byTab.put(tabRodizio.tabId(), tabRodizio);
    }

    @Override
    public void updateStatus(TabRodizio tabRodizio) {
        byTab.put(tabRodizio.tabId(), tabRodizio);
    }

    /** A tela dos passadores junta a mesa e o setor: quem confere é o teste de integração. */
    @Override
    public ResultPage<PassagemTableView> findOpenPassagem(EstablishmentId establishmentId, PageQuery page) {
        throw new UnsupportedOperationException();
    }
}
