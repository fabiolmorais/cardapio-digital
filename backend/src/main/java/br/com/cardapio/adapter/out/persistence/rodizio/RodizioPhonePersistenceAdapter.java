package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rodizio.model.RodizioParticipation;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Os celulares no rodízio. A escrita é SQL nativo (o filtro do Hibernate não vale nele): o
 * restaurante vai em cada consulta, e o da sessão é conferido no WHERE da ativação.
 */
@Component
class RodizioPhonePersistenceAdapter implements RodizioPhonePort {

    private final RodizioPhoneJpaRepository phones;
    private final EstablishmentFilter establishmentFilter;

    RodizioPhonePersistenceAdapter(RodizioPhoneJpaRepository phones, EstablishmentFilter establishmentFilter) {
        this.phones = phones;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void joinAllAtTable(EstablishmentId establishmentId, TabRodizioId rodizio, DiningTableId tableId,
                               Instant now) {
        phones.joinAllAtTable(establishmentId.value(), rodizio.value(), tableId.value(), now);
    }

    @Override
    public void request(EstablishmentId establishmentId, TabRodizioId rodizio, TableSessionId session, Instant now) {
        phones.request(establishmentId.value(), rodizio.value(), session.value(), now);
    }

    @Override
    public int admitRequested(EstablishmentId establishmentId, TabRodizioId rodizio, Instant now) {
        return phones.admitRequested(establishmentId.value(), rodizio.value(), now);
    }

    @Override
    public int cancelRequests(EstablishmentId establishmentId, TabRodizioId rodizio) {
        return phones.cancelRequests(establishmentId.value(), rodizio.value());
    }

    @Override
    public RodizioParticipation find(EstablishmentId establishmentId, TabRodizioId rodizio, TableSessionId session) {
        establishmentFilter.enable(establishmentId);
        return phones.findByEstablishmentIdAndTabRodizioIdAndSessionId(establishmentId.value(), rodizio.value(),
                        session.value())
                .map(p -> p.getJoinedAt() != null ? RodizioParticipation.JOINED : RodizioParticipation.REQUESTED)
                .orElse(RodizioParticipation.NONE);
    }
}
