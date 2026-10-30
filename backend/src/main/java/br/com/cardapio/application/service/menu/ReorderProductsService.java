package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ReorderProductsUseCase;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.model.DisplayOrder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class ReorderProductsService implements ReorderProductsUseCase {

    private final MenuCategoryPort categories;
    private final ProductPort products;

    public ReorderProductsService(MenuCategoryPort categories, ProductPort products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    @Transactional
    public void reorder(EstablishmentId establishmentId, MenuCategoryId categoryId, List<ProductId> ids) {
        MenuLookups.activeCategory(categories, establishmentId, categoryId);
        DisplayOrder<ProductId> order = new DisplayOrder<>(ids);
        List<Product> current = products.findActiveByCategory(establishmentId, categoryId);
        order.requireSameItemsAs(current.stream().map(Product::id).toList());

        for (Product product : current) {
            int position = order.positionOf(product.id());
            if (position != product.position()) {
                products.save(product.at(position));
            }
        }
    }
}
