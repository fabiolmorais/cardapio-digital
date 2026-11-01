package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.RemoveProductMediaUseCase;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.ProductMediaNotFoundException;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import org.springframework.transaction.annotation.Transactional;

public class RemoveProductMediaService implements RemoveProductMediaUseCase {

    private final ProductMediaPort media;

    public RemoveProductMediaService(ProductMediaPort media) {
        this.media = media;
    }

    @Override
    @Transactional
    public void remove(EstablishmentId establishmentId, ProductId productId, ProductMediaId mediaId) {
        // A foto precisa ser deste produto: o id da URL não pode apontar para a foto de outro.
        ProductMedia found = media.find(establishmentId, mediaId)
                .filter(m -> m.productId().equals(productId))
                .orElseThrow(ProductMediaNotFoundException::new);
        if (found.active()) {
            media.save(found.deactivate());
        }
    }
}
