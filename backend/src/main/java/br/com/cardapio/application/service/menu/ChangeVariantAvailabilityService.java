package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ChangeVariantAvailabilityUseCase;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.ProductVariantNotFoundException;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import org.springframework.transaction.annotation.Transactional;

/** "Acabou a massa de broto": só aquele tamanho esgota; os outros continuam. */
public class ChangeVariantAvailabilityService implements ChangeVariantAvailabilityUseCase {

    private final ProductOptionsPort options;

    public ChangeVariantAvailabilityService(ProductOptionsPort options) {
        this.options = options;
    }

    @Override
    @Transactional
    public ProductVariant change(EstablishmentId establishmentId, ProductVariantId id, boolean available) {
        ProductVariant variant = options.findVariant(establishmentId, id)
                .filter(ProductVariant::active)
                .orElseThrow(ProductVariantNotFoundException::new);
        ProductVariant changed = variant.withAvailability(available);
        options.saveVariant(changed);
        return changed;
    }
}
