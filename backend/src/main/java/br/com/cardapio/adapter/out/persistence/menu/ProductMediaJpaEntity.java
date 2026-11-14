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

    /** O produto dono; null quando a galeria é de uma opção. */
    @Column(name = "product_id")
    private UUID productId;

    /** A opção dona (o sabor da pizza); null quando a galeria é de um produto. */
    @Column(name = "option_item_id")
    private UUID optionItemId;

    @Column(nullable = false)
    private String type;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    /** Null no vídeo ainda em conversão (o domínio usa 0). */
    private Integer width;

    private Integer height;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String status;

    @Column(name = "video_id")
    private String videoId;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    private String failure;

    protected ProductMediaJpaEntity() {
    }

    ProductMediaJpaEntity(UUID id, UUID establishmentId, UUID productId, UUID optionItemId, String type, String storagePath,
                          Integer width, Integer height, int position, boolean active, String status, String videoId,
                          Integer durationSeconds, String failure) {
        super(establishmentId);
        this.id = id;
        this.productId = productId;
        this.optionItemId = optionItemId;
        this.type = type;
        this.storagePath = storagePath;
        this.width = width;
        this.height = height;
        this.position = position;
        this.active = active;
        this.status = status;
        this.videoId = videoId;
        this.durationSeconds = durationSeconds;
        this.failure = failure;
    }

    UUID getId() { return id; }
    UUID getProductId() { return productId; }
    UUID getOptionItemId() { return optionItemId; }
    String getType() { return type; }
    String getStoragePath() { return storagePath; }
    Integer getWidth() { return width; }
    Integer getHeight() { return height; }
    int getPosition() { return position; }
    boolean isActive() { return active; }
    String getStatus() { return status; }
    String getVideoId() { return videoId; }
    Integer getDurationSeconds() { return durationSeconds; }
    String getFailure() { return failure; }
}
