package br.com.cardapio.adapter.out.persistence.floor;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sector")
class SectorJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active;

    protected SectorJpaEntity() {
    }

    SectorJpaEntity(UUID id, UUID establishmentId, String name, int position, boolean active) {
        super(establishmentId);
        this.id = id;
        this.name = name;
        this.position = position;
        this.active = active;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    int getPosition() { return position; }
    boolean isActive() { return active; }
}
