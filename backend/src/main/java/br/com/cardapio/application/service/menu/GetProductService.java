package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.GetProductUseCase;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import org.springframework.transaction.annotation.Transactional;

public class GetProductService implements GetProductUseCase {

    private final ProductPort products;

    public GetProductService(ProductPort products) {
        this.products = products;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSummary get(EstablishmentId establishmentId, ProductId productId) {
        return ProductSummary.from(MenuLookups.activeProduct(products, establishmentId, productId));
    }
}
