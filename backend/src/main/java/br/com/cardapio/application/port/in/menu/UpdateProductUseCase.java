package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;

public interface UpdateProductUseCase {

    /** Troca tudo o que o formulário tem (é o PUT): o que vier vazio, como a promoção, é apagado. */
    ProductSummary update(EstablishmentId establishmentId, ProductId productId, ProductForm form);
}
