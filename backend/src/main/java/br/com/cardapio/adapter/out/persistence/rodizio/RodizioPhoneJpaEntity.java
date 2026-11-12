package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Um celular no rodízio da comanda. A escrita é por SQL nativo (ON CONFLICT); a entidade serve para ler. */
@Entity
@Table(name = "rodizio_phone")
class RodizioPhoneJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "tab_rodizio_id", nullable = false)
    private UUID tabRodizioId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    protected RodizioPhoneJpaEntity() {
    }

    Instant getRequestedAt() { return requestedAt; }
    Instant getJoinedAt() { return joinedAt; }
}
