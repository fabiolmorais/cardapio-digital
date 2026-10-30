package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;

public interface ChangeProductAvailabilityUseCase {

    /** O interruptor da lista: disponível ou "Esgotado hoje". */
    ProductSummary change(EstablishmentId establishmentId, ProductId productId, boolean available);
}
