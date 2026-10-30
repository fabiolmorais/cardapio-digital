package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;

public interface GetProductUseCase {

    ProductSummary get(EstablishmentId establishmentId, ProductId productId);
}
