package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "product_media")
class ProductMediaJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String type;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active;

    protected ProductMediaJpaEntity() {
    }

    ProductMediaJpaEntity(UUID id, UUID establishmentId, UUID productId, String type, String storagePath, int width,
                          int height, int position, boolean active) {
        super(establishmentId);
        this.id = id;
        this.productId = productId;
        this.type = type;
        this.storagePath = storagePath;
        this.width = width;
        this.height = height;
        this.position = position;
        this.active = active;
    }

    UUID getId() { return id; }
    UUID getProductId() { return productId; }
    String getType() { return type; }
    String getStoragePath() { return storagePath; }
    int getWidth() { return width; }
    int getHeight() { return height; }
    int getPosition() { return position; }
    boolean isActive() { return active; }
}
