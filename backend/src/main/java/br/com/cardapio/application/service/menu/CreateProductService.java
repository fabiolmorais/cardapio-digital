package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.CreateProductUseCase;
import br.com.cardapio.application.port.in.menu.ProductForm;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.Product;
import org.springframework.transaction.annotation.Transactional;

public class CreateProductService implements CreateProductUseCase {

    private final MenuCategoryPort categories;
    private final ProductPort products;

    public CreateProductService(MenuCategoryPort categories, ProductPort products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    @Transactional
    public ProductSummary create(EstablishmentId establishmentId, ProductForm form) {
        // A categoria precisa ser deste restaurante e estar ativa: um id de outro restaurante dá 404.
        MenuCategory category = MenuLookups.activeCategory(categories, establishmentId, form.categoryId());
        Product product = Product.create(establishmentId, category.id(), form.details(), form.available(),
                products.nextPosition(establishmentId, category.id()));
        products.save(product);
        return ProductSummary.from(product);
    }
}
