package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** O mínimo e o máximo de um grupo num tamanho do produto. */
@Entity
@Table(name = "product_option_group_limit")
class ProductOptionGroupLimitJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "option_group_id", nullable = false)
    private UUID optionGroupId;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "min_choices", nullable = false)
    private int minChoices;

    @Column(name = "max_choices", nullable = false)
    private int maxChoices;

    protected ProductOptionGroupLimitJpaEntity() {
    }

    ProductOptionGroupLimitJpaEntity(UUID id, UUID establishmentId, UUID productId, UUID optionGroupId, UUID productVariantId, int minChoices, int maxChoices) {
        super(establishmentId);
        this.id = id;
        this.productId = productId;
        this.optionGroupId = optionGroupId;
        this.productVariantId = productVariantId;
        this.minChoices = minChoices;
        this.maxChoices = maxChoices;
    }

    UUID getId() { return id; }
    UUID getProductId() { return productId; }
    UUID getOptionGroupId() { return optionGroupId; }
    UUID getProductVariantId() { return productVariantId; }
    int getMinChoices() { return minChoices; }
    int getMaxChoices() { return maxChoices; }
}
