package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ProductForm;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.in.menu.UpdateProductUseCase;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import org.springframework.transaction.annotation.Transactional;

public class UpdateProductService implements UpdateProductUseCase {

    private final MenuCategoryPort categories;
    private final ProductPort products;

    public UpdateProductService(MenuCategoryPort categories, ProductPort products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    @Transactional
    public ProductSummary update(EstablishmentId establishmentId, ProductId productId, ProductForm form) {
        Product product = MenuLookups.activeProduct(products, establishmentId, productId).edit(form.details());

        if (!product.categoryId().equals(form.categoryId())) {
            MenuLookups.activeCategory(categories, establishmentId, form.categoryId());
            product = product.moveTo(form.categoryId(), products.nextPosition(establishmentId, form.categoryId()));
        }
        product = form.available() ? product.markAvailable() : product.markSoldOut();

        products.save(product);
        return ProductSummary.from(product);
    }
}
