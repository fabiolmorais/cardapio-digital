package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Um grupo ligado a um produto, na ordem em que o cliente vê. */
@Entity
@Table(name = "product_option_group")
class ProductOptionGroupJpaEntity extends EstablishmentScopedEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "option_group_id", nullable = false)
    private UUID optionGroupId;

    @Column(name = "position", nullable = false)
    private int position;

    protected ProductOptionGroupJpaEntity() {
    }

    ProductOptionGroupJpaEntity(UUID id, UUID establishmentId, UUID productId, UUID optionGroupId, int position) {
        super(establishmentId);
        this.id = id;
        this.productId = productId;
        this.optionGroupId = optionGroupId;
        this.position = position;
    }

    UUID getId() { return id; }
    UUID getProductId() { return productId; }
    UUID getOptionGroupId() { return optionGroupId; }
    int getPosition() { return position; }
}
