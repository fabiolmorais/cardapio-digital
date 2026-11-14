package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ReorderProductMediaUseCase;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MediaOwner;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import br.com.cardapio.domain.shared.model.DisplayOrder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** O mesmo DisplayOrder do Guia 9, agora para as fotos. */
public class ReorderProductMediaService implements ReorderProductMediaUseCase {

    private final ProductPort products;
    private final OptionGroupPort groups;
    private final ProductMediaPort media;

    public ReorderProductMediaService(ProductPort products, OptionGroupPort groups, ProductMediaPort media) {
        this.products = products;
        this.groups = groups;
        this.media = media;
    }

    @Override
    @Transactional
    public void reorder(EstablishmentId establishmentId, MediaOwner owner, List<ProductMediaId> ids) {
        MediaOwners.requireActive(products, groups, establishmentId, owner);
        DisplayOrder<ProductMediaId> order = new DisplayOrder<>(ids);
        List<ProductMedia> current = media.findActiveByOwner(establishmentId, owner);
        order.requireSameItemsAs(current.stream().map(ProductMedia::id).toList());

        for (ProductMedia item : current) {
            int position = order.positionOf(item.id());
            if (position != item.position()) {
                media.save(item.at(position));
            }
        }
    }
}
