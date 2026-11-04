package br.com.cardapio.adapter.out.persistence.rodizio;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

/**
 * Um produto incluído no rodízio. A chave é o par (rodízio, produto): o mesmo produto não entra
 * duas vezes no mesmo rodízio. A @IdClass diz ao JPA qual classe representa esse par.
 */
@Entity
@Table(name = "rodizio_product")
@IdClass(RodizioProductJpaEntity.Key.class)
class RodizioProductJpaEntity extends EstablishmentScopedEntity {

    record Key(UUID rodizioId, UUID productId) implements Serializable {
    }

    @Id
    @Column(name = "rodizio_id")
    private UUID rodizioId;

    @Id
    @Column(name = "product_id")
    private UUID productId;

    protected RodizioProductJpaEntity() {
    }

    RodizioProductJpaEntity(UUID rodizioId, UUID productId, UUID establishmentId) {
        super(establishmentId);
        this.rodizioId = rodizioId;
        this.productId = productId;
    }

    UUID getRodizioId() { return rodizioId; }
    UUID getProductId() { return productId; }
}
