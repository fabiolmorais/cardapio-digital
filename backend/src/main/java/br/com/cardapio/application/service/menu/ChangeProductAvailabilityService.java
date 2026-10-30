package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ChangeProductAvailabilityUseCase;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import org.springframework.transaction.annotation.Transactional;

public class ChangeProductAvailabilityService implements ChangeProductAvailabilityUseCase {

    private final ProductPort products;

    public ChangeProductAvailabilityService(ProductPort products) {
        this.products = products;
    }

    @Override
    @Transactional
    public ProductSummary change(EstablishmentId establishmentId, ProductId productId, boolean available) {
        Product product = MenuLookups.activeProduct(products, establishmentId, productId);
        Product changed = available ? product.markAvailable() : product.markSoldOut();
        products.save(changed);
        return ProductSummary.from(changed);
    }
}
