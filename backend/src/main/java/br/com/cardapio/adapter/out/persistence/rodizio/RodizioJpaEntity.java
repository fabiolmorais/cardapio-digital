package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rodizio")
class RodizioJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String description;

    @Column(name = "max_per_item_per_round")
    private Integer maxPerItemPerRound;

    @Column(name = "max_pending_per_guest")
    private Integer maxPendingPerGuest;

    @Column(name = "min_round_interval_minutes")
    private Integer minRoundIntervalMinutes;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "waste_notice")
    private String wasteNotice;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RodizioJpaEntity() {
    }

    RodizioJpaEntity(UUID id, UUID establishmentId, String name, String type, String description,
                     Integer maxPerItemPerRound, Integer maxPendingPerGuest, Integer minRoundIntervalMinutes,
                     Integer timeLimitMinutes, String wasteNotice, boolean active, Instant createdAt) {
        super(establishmentId);
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.maxPerItemPerRound = maxPerItemPerRound;
        this.maxPendingPerGuest = maxPendingPerGuest;
        this.minRoundIntervalMinutes = minRoundIntervalMinutes;
        this.timeLimitMinutes = timeLimitMinutes;
        this.wasteNotice = wasteNotice;
        this.active = active;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getType() { return type; }
    String getDescription() { return description; }
    Integer getMaxPerItemPerRound() { return maxPerItemPerRound; }
    Integer getMaxPendingPerGuest() { return maxPendingPerGuest; }
    Integer getMinRoundIntervalMinutes() { return minRoundIntervalMinutes; }
    Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    String getWasteNotice() { return wasteNotice; }
    boolean isActive() { return active; }
    Instant getCreatedAt() { return createdAt; }
}
