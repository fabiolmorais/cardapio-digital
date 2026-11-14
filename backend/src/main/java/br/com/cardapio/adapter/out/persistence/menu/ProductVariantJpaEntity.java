package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Um tamanho do produto (ADR 0010). Nunca é apagado: sai desativado. */
@Entity
@Table(name = "product_variant")
class ProductVariantJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "promo_price_cents")
    private Long promoPriceCents;

    @Column(name = "available", nullable = false)
    private boolean available;

    @Column(name = "rodizio_default", nullable = false)
    private boolean rodizioDefault;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ProductVariantJpaEntity() {
    }

    ProductVariantJpaEntity(UUID id, UUID establishmentId, UUID productId, String name, long priceCents, Long promoPriceCents, boolean available, boolean rodizioDefault, int position, boolean active) {
        super(establishmentId);
        this.id = id;
        this.productId = productId;
        this.name = name;
        this.priceCents = priceCents;
        this.promoPriceCents = promoPriceCents;
        this.available = available;
        this.rodizioDefault = rodizioDefault;
        this.position = position;
        this.active = active;
    }

    UUID getId() { return id; }
    UUID getProductId() { return productId; }
    String getName() { return name; }
    long getPriceCents() { return priceCents; }
    Long getPromoPriceCents() { return promoPriceCents; }
    boolean isAvailable() { return available; }
    boolean isRodizioDefault() { return rodizioDefault; }
    int getPosition() { return position; }
    boolean isActive() { return active; }
}
