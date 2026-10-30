package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.DeactivateProductUseCase;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.ProductNotFoundException;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import org.springframework.transaction.annotation.Transactional;

public class DeactivateProductService implements DeactivateProductUseCase {

    private final ProductPort products;

    public DeactivateProductService(ProductPort products) {
        this.products = products;
    }

    /** O registro fica: os pedidos antigos (Etapa 8) continuam apontando para ele. */
    @Override
    @Transactional
    public void deactivate(EstablishmentId establishmentId, ProductId productId) {
        Product product = products.find(establishmentId, productId).orElseThrow(ProductNotFoundException::new);
        if (product.active()) {
            products.save(product.deactivate());
        }
    }
}
