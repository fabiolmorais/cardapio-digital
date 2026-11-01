package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MediaType;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class ProductMediaPersistenceAdapter implements ProductMediaPort {

    private final ProductMediaJpaRepository media;
    private final EstablishmentFilter establishmentFilter;

    ProductMediaPersistenceAdapter(ProductMediaJpaRepository media, EstablishmentFilter establishmentFilter) {
        this.media = media;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void save(ProductMedia m) {
        establishmentFilter.enable(m.establishmentId());
        media.save(new ProductMediaJpaEntity(m.id().value(), m.establishmentId().value(), m.productId().value(),
                m.type().name(), m.storagePath(), m.width(), m.height(), m.position(), m.active()));
    }

    @Override
    public Optional<ProductMedia> find(EstablishmentId establishmentId, ProductMediaId id) {
        establishmentFilter.enable(establishmentId);
        return media.findByIdAndEstablishmentId(id.value(), establishmentId.value()).map(this::toDomain);
    }

    @Override
    public List<ProductMedia> findActiveByProduct(EstablishmentId establishmentId, ProductId productId) {
        establishmentFilter.enable(establishmentId);
        return media.findByEstablishmentIdAndProductIdAndActiveTrueOrderByPositionAscIdAsc(
                        establishmentId.value(), productId.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId, ProductId productId) {
        establishmentFilter.enable(establishmentId);
        return media.nextPosition(establishmentId.value(), productId.value());
    }

    private ProductMedia toDomain(ProductMediaJpaEntity e) {
        return new ProductMedia(new ProductMediaId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new ProductId(e.getProductId()), MediaType.valueOf(e.getType()), e.getStoragePath(), e.getWidth(),
                e.getHeight(), e.getPosition(), e.isActive());
    }
}
