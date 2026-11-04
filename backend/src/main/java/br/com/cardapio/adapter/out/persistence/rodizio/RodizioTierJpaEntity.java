package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "rodizio_tier")
class RodizioTierJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "rodizio_id", nullable = false)
    private UUID rodizioId;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_price_cents", nullable = false)
    private long defaultPriceCents;

    @Column(nullable = false)
    private int position;

    protected RodizioTierJpaEntity() {
    }

    RodizioTierJpaEntity(UUID id, UUID establishmentId, UUID rodizioId, String name, long defaultPriceCents,
                         int position) {
        super(establishmentId);
        this.id = id;
        this.rodizioId = rodizioId;
        this.name = name;
        this.defaultPriceCents = defaultPriceCents;
        this.position = position;
    }

    UUID getId() { return id; }
    UUID getRodizioId() { return rodizioId; }
    String getName() { return name; }
    long getDefaultPriceCents() { return defaultPriceCents; }
    int getPosition() { return position; }
}
