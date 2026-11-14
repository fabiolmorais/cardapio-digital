package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.out.menu.OptionGroupPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.OptionItemNotFoundException;
import br.com.cardapio.domain.menu.model.MediaOwner;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductId;

/**
 * "O dono da galeria existe, é deste restaurante e está ativo?": o produto, ou a opção (num grupo
 * ativo). Desativado conta como inexistente: 404.
 */
final class MediaOwners {

    private MediaOwners() {
    }

    /** O nome do dono (o título do vídeo no serviço de vídeo). */
    static String requireActive(ProductPort products, OptionGroupPort groups, EstablishmentId establishmentId,
                                MediaOwner owner) {
        return switch (owner) {
            case ProductId product -> MenuLookups.activeProduct(products, establishmentId, product).details().name();
            case OptionItemId option -> groups.findItem(establishmentId, option)
                    .filter(OptionItem::active)
                    .filter(item -> groups.find(establishmentId, item.groupId())
                            .map(g -> g.group().active()).orElse(false))
                    .orElseThrow(OptionItemNotFoundException::new)
                    .name();
        };
    }
}
