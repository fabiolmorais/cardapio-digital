package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.menu.MediaCount;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.MediaType;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                m.type().name(), m.storagePath(), m.width() > 0 ? m.width() : null, m.height() > 0 ? m.height() : null,
                m.position(), m.active(), m.status().name(), m.videoId(), m.durationSeconds(), m.failure()));
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

    @Override
    public List<ProductMedia> findReadyGalleries(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return media.findByEstablishmentIdAndActiveTrueAndStatusOrderByProductIdAscPositionAscIdAsc(
                establishmentId.value(), MediaStatus.READY.name()).stream().map(this::toDomain).toList();
    }

    @Override
    public Map<ProductId, MediaCount> countByProducts(EstablishmentId establishmentId,
                                                      Collection<ProductId> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        establishmentFilter.enable(establishmentId);
        Map<ProductId, MediaCount> counts = new HashMap<>();
        for (MediaCountRow row : media.countByProducts(establishmentId.value(),
                productIds.stream().map(ProductId::value).toList())) {
            int total = Math.toIntExact(row.getTotal());
            counts.merge(new ProductId(row.getProductId()),
                    MediaType.VIDEO.name().equals(row.getType()) ? new MediaCount(0, total) : new MediaCount(total, 0),
                    (a, b) -> new MediaCount(a.photos() + b.photos(), a.videos() + b.videos()));
        }
        return counts;
    }

    /** Sem o filtro por restaurante: o aviso do serviço de vídeo não diz de qual restaurante é. */
    @Override
    public Optional<ProductMedia> findByVideoId(String videoId) {
        return media.findByVideoId(videoId).map(this::toDomain);
    }

    private ProductMedia toDomain(ProductMediaJpaEntity e) {
        return new ProductMedia(new ProductMediaId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new ProductId(e.getProductId()), MediaType.valueOf(e.getType()), e.getStoragePath(),
                e.getWidth() == null ? 0 : e.getWidth(), e.getHeight() == null ? 0 : e.getHeight(), e.getPosition(),
                e.isActive(), MediaStatus.valueOf(e.getStatus()), e.getVideoId(), e.getDurationSeconds(),
                e.getFailure());
    }
}
