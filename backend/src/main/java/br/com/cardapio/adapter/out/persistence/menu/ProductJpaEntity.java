package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "product")
class ProductJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    /** Long (e não long): null quando não há promoção. */
    @Column(name = "promo_price_cents")
    private Long promoPriceCents;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active;

    protected ProductJpaEntity() {
    }

    ProductJpaEntity(UUID id, UUID establishmentId, UUID categoryId, String name, String description, long priceCents,
                     Long promoPriceCents, boolean featured, boolean available, int position, boolean active) {
        super(establishmentId);
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.promoPriceCents = promoPriceCents;
        this.featured = featured;
        this.available = available;
        this.position = position;
        this.active = active;
    }

    UUID getId() { return id; }
    UUID getCategoryId() { return categoryId; }
    String getName() { return name; }
    String getDescription() { return description; }
    long getPriceCents() { return priceCents; }
    Long getPromoPriceCents() { return promoPriceCents; }
    boolean isFeatured() { return featured; }
    boolean isAvailable() { return available; }
    int getPosition() { return position; }
    boolean isActive() { return active; }
}
