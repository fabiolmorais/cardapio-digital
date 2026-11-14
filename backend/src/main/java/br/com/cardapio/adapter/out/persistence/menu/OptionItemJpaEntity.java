package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Uma opção de um grupo (ADR 0010). Nunca é apagada: sai desativada. */
@Entity
@Table(name = "option_item")
class OptionItemJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "option_group_id", nullable = false)
    private UUID optionGroupId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "available", nullable = false)
    private boolean available;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected OptionItemJpaEntity() {
    }

    OptionItemJpaEntity(UUID id, UUID establishmentId, UUID optionGroupId, String name, long priceCents, boolean available, int position, boolean active) {
        super(establishmentId);
        this.id = id;
        this.optionGroupId = optionGroupId;
        this.name = name;
        this.priceCents = priceCents;
        this.available = available;
        this.position = position;
        this.active = active;
    }

    UUID getId() { return id; }
    UUID getOptionGroupId() { return optionGroupId; }
    String getName() { return name; }
    long getPriceCents() { return priceCents; }
    boolean isAvailable() { return available; }
    int getPosition() { return position; }
    boolean isActive() { return active; }
}
