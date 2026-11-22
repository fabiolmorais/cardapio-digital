package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ProductForm;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.in.menu.UpdateProductUseCase;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.menu.ProductStatusHistoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductStatusChange;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class UpdateProductService implements UpdateProductUseCase {

    private final MenuCategoryPort categories;
    private final ProductPort products;
    private final ProductStatusHistoryPort history;
    private final Clock clock;

    public UpdateProductService(MenuCategoryPort categories, ProductPort products, ProductStatusHistoryPort history,
                                Clock clock) {
        this.categories = categories;
        this.products = products;
        this.history = history;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProductSummary update(EstablishmentId establishmentId, ProductId productId, ProductForm form) {
        Product before = MenuLookups.activeProduct(products, establishmentId, productId);
        Product product = before.edit(form.details());

        if (!product.categoryId().equals(form.categoryId())) {
            MenuLookups.activeCategory(categories, establishmentId, form.categoryId());
            product = product.moveTo(form.categoryId(), products.nextPosition(establishmentId, form.categoryId()));
        }
        product = form.available() ? product.markAvailable() : product.markSoldOut();

        products.save(product);
        history.record(ProductStatusChange.between(before, product, clock.instant()));
        return ProductSummary.from(product);
    }
}
