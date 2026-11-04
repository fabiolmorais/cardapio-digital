package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.model.GuestTier;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class TabRodizioPersistenceAdapter implements TabRodizioPort {

    private final TabRodizioJpaRepository rodizios;
    private final TabRodizioGuestJpaRepository guests;
    private final EstablishmentFilter establishmentFilter;

    TabRodizioPersistenceAdapter(TabRodizioJpaRepository rodizios, TabRodizioGuestJpaRepository guests,
                                 EstablishmentFilter establishmentFilter) {
        this.rodizios = rodizios;
        this.guests = guests;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public boolean insertIfNone(TabRodizio r) {
        establishmentFilter.enable(r.establishmentId());
        UUID id = r.id().value();
        UUID establishment = r.establishmentId().value();
        if (rodizios.insertIfNone(id, establishment, r.tabId().value(), r.rodizioId().value(), r.rodizioName(),
                r.activatedBy().value(), r.startedAt(), r.endsAt()) == 0) {
            return false;
        }
        List<TabRodizioGuestJpaEntity> rows = new ArrayList<>();
        for (int position = 0; position < r.guests().size(); position++) {
            GuestTier g = r.guests().get(position);
            rows.add(new TabRodizioGuestJpaEntity(id, position, establishment, g.tierName(), g.quantity(),
                    g.unitPrice().cents()));
        }
        guests.saveAll(rows);
        return true;
    }

    @Override
    public Optional<TabRodizio> findByTab(EstablishmentId establishmentId, TabId tabId) {
        establishmentFilter.enable(establishmentId);
        return rodizios.findByTabIdAndEstablishmentId(tabId.value(), establishmentId.value())
                .map(e -> new TabRodizio(new TabRodizioId(e.getId()), establishmentId, new TabId(e.getTabId()),
                        new RodizioId(e.getRodizioId()), e.getRodizioName(),
                        guests.findByTabRodizioIdOrderByPositionAsc(e.getId()).stream()
                                .map(g -> new GuestTier(g.getTierName(), g.getQuantity(),
                                        Money.ofCents(g.getUnitPriceCents())))
                                .toList(),
                        new UserId(e.getActivatedBy()), e.getStartedAt(), e.getEndsAt()));
    }

    @Override
    public void updateGuests(TabRodizio r) {
        establishmentFilter.enable(r.establishmentId());
        for (int position = 0; position < r.guests().size(); position++) {
            guests.updateQuantity(r.id().value(), position, r.establishmentId().value(),
                    r.guests().get(position).quantity());
        }
    }
}
