package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** O preço de uma opção num tamanho do produto. */
@Entity
@Table(name = "option_item_price")
class OptionItemPriceJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "option_item_id", nullable = false)
    private UUID optionItemId;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    protected OptionItemPriceJpaEntity() {
    }

    OptionItemPriceJpaEntity(UUID id, UUID establishmentId, UUID productId, UUID optionItemId, UUID productVariantId, long priceCents) {
        super(establishmentId);
        this.id = id;
        this.productId = productId;
        this.optionItemId = optionItemId;
        this.productVariantId = productVariantId;
        this.priceCents = priceCents;
    }

    UUID getId() { return id; }
    UUID getProductId() { return productId; }
    UUID getOptionItemId() { return optionItemId; }
    UUID getProductVariantId() { return productVariantId; }
    long getPriceCents() { return priceCents; }
}
