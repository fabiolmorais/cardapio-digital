package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ChangeProductAvailabilityUseCase;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.menu.ProductStatusHistoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductStatusChange;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class ChangeProductAvailabilityService implements ChangeProductAvailabilityUseCase {

    private final ProductPort products;
    private final ProductStatusHistoryPort history;
    private final Clock clock;

    public ChangeProductAvailabilityService(ProductPort products, ProductStatusHistoryPort history, Clock clock) {
        this.products = products;
        this.history = history;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProductSummary change(EstablishmentId establishmentId, ProductId productId, boolean available) {
        Product product = MenuLookups.activeProduct(products, establishmentId, productId);
        Product changed = available ? product.markAvailable() : product.markSoldOut();
        products.save(changed);
        history.record(ProductStatusChange.between(product, changed, clock.instant()));
        return ProductSummary.from(changed);
    }
}
