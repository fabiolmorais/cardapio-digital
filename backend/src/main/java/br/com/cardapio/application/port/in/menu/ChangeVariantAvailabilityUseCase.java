package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;

public interface ChangeVariantAvailabilityUseCase {

    ProductVariant change(EstablishmentId establishmentId, ProductVariantId id, boolean available);
}
