package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "tab_rodizio_guest")
@IdClass(TabRodizioGuestJpaEntity.Key.class)
class TabRodizioGuestJpaEntity extends EstablishmentScopedEntity {

    record Key(UUID tabRodizioId, int position) implements Serializable {
    }

    @Id
    @Column(name = "tab_rodizio_id")
    private UUID tabRodizioId;

    @Id
    private int position;

    @Column(name = "tier_name", nullable = false)
    private String tierName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    protected TabRodizioGuestJpaEntity() {
    }

    TabRodizioGuestJpaEntity(UUID tabRodizioId, int position, UUID establishmentId, String tierName, int quantity,
                             long unitPriceCents) {
        super(establishmentId);
        this.tabRodizioId = tabRodizioId;
        this.position = position;
        this.tierName = tierName;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    UUID getTabRodizioId() { return tabRodizioId; }
    String getTierName() { return tierName; }
    int getQuantity() { return quantity; }
    long getUnitPriceCents() { return unitPriceCents; }
}
