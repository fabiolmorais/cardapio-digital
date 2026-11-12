package br.com.cardapio.support;

import br.com.cardapio.application.port.out.rodizio.PassagemTableView;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.rodizio.model.RodizioParticipation;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Os rodízios do cadastro e os das comandas, na memória. */
public final class InMemoryRodizios implements RodizioPort, TabRodizioPort, RodizioPhonePort {

    public final Map<RodizioId, Rodizio> rodizios = new LinkedHashMap<>();
    public final Map<TabId, TabRodizio> byTab = new LinkedHashMap<>();
    /**
     * Os rodízios em que todo celular conta como dentro (a ativação). Para simular quem chegou
     * depois, o teste tira o rodízio daqui: aí só vale o que está em {@link #phones}.
     */
    public final Set<TabRodizioId> openToAll = new HashSet<>();
    public final Map<TabRodizioId, Map<TableSessionId, RodizioParticipation>> phones = new HashMap<>();

    // --- RodizioPhonePort
    @Override
    public void joinAllAtTable(EstablishmentId establishmentId, TabRodizioId rodizio, DiningTableId tableId,
                               Instant now) {
        openToAll.add(rodizio);
    }

    @Override
    public void request(EstablishmentId establishmentId, TabRodizioId rodizio, TableSessionId session, Instant now) {
        phones.computeIfAbsent(rodizio, r -> new HashMap<>()).putIfAbsent(session, RodizioParticipation.REQUESTED);
    }

    @Override
    public int admitRequested(EstablishmentId establishmentId, TabRodizioId rodizio, Instant now) {
        Map<TableSessionId, RodizioParticipation> those = phones.getOrDefault(rodizio, Map.of());
        int admitted = 0;
        for (var entry : those.entrySet()) {
            if (entry.getValue() == RodizioParticipation.REQUESTED) {
                entry.setValue(RodizioParticipation.JOINED);
                admitted++;
            }
        }
        return admitted;
    }

    @Override
    public RodizioParticipation find(EstablishmentId establishmentId, TabRodizioId rodizio, TableSessionId session) {
        RodizioParticipation explicit = phones.getOrDefault(rodizio, Map.of()).get(session);
        if (explicit != null) {
            return explicit;
        }
        return openToAll.contains(rodizio) ? RodizioParticipation.JOINED : RodizioParticipation.NONE;
    }

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
