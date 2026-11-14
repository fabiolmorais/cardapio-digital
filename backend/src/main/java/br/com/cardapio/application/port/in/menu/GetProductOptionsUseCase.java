package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;

public interface GetProductOptionsUseCase {

    ProductOptions get(EstablishmentId establishmentId, ProductId productId);
}
