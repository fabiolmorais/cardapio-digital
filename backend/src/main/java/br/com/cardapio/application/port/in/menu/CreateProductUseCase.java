package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface CreateProductUseCase {

    ProductSummary create(EstablishmentId establishmentId, ProductForm form);
}
