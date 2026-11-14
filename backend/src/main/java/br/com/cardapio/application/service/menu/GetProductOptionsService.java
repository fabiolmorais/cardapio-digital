package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.GetProductOptionsUseCase;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import org.springframework.transaction.annotation.Transactional;

public class GetProductOptionsService implements GetProductOptionsUseCase {

    private final ProductPort products;
    private final ProductOptionsPort options;

    public GetProductOptionsService(ProductPort products, ProductOptionsPort options) {
        this.products = products;
        this.options = options;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductOptions get(EstablishmentId establishmentId, ProductId productId) {
        MenuLookups.activeProduct(products, establishmentId, productId);
        return options.find(establishmentId, productId);
    }
}
