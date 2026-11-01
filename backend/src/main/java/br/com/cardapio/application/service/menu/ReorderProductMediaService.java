package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ReorderProductMediaUseCase;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import br.com.cardapio.domain.shared.model.DisplayOrder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** O mesmo DisplayOrder do Guia 9, agora para as fotos. */
public class ReorderProductMediaService implements ReorderProductMediaUseCase {

    private final ProductPort products;
    private final ProductMediaPort media;

    public ReorderProductMediaService(ProductPort products, ProductMediaPort media) {
        this.products = products;
        this.media = media;
    }

    @Override
    @Transactional
    public void reorder(EstablishmentId establishmentId, ProductId productId, List<ProductMediaId> ids) {
        MenuLookups.activeProduct(products, establishmentId, productId);
        DisplayOrder<ProductMediaId> order = new DisplayOrder<>(ids);
        List<ProductMedia> current = media.findActiveByProduct(establishmentId, productId);
        order.requireSameItemsAs(current.stream().map(ProductMedia::id).toList());

        for (ProductMedia item : current) {
            int position = order.positionOf(item.id());
            if (position != item.position()) {
                media.save(item.at(position));
            }
        }
    }
}
