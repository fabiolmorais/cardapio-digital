package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tab_rodizio")
class TabRodizioJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "tab_id", nullable = false)
    private UUID tabId;

    @Column(name = "rodizio_id", nullable = false)
    private UUID rodizioId;

    @Column(name = "rodizio_name", nullable = false)
    private String rodizioName;

    @Column(name = "activated_by", nullable = false)
    private UUID activatedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    protected TabRodizioJpaEntity() {
    }

    UUID getId() { return id; }
    UUID getTabId() { return tabId; }
    UUID getRodizioId() { return rodizioId; }
    String getRodizioName() { return rodizioName; }
    UUID getActivatedBy() { return activatedBy; }
    Instant getStartedAt() { return startedAt; }
}
