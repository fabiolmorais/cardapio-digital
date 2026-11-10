package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.application.port.out.menu.PopularProductsPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Os mais pedidos, numa consulta nativa só (o restaurante vai na própria consulta). */
@Component
class PopularProductsPersistenceAdapter implements PopularProductsPort {

    private final OrderItemJpaRepository items;

    PopularProductsPersistenceAdapter(OrderItemJpaRepository items) {
        this.items = items;
    }

    @Override
    public List<ProductId> findMostOrdered(EstablishmentId establishmentId, Instant since, int limit) {
        return items.findMostOrdered(establishmentId.value(), since, limit).stream().map(ProductId::new).toList();
    }
}
