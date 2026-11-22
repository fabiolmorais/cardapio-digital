package br.com.cardapio.application.service.menu;

import br.com.cardapio.domain.menu.exception.MenuFullException;
import br.com.cardapio.application.port.in.menu.CreateProductUseCase;
import br.com.cardapio.application.port.in.menu.ProductForm;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.menu.ProductStatusHistoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductStatusChange;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class CreateProductService implements CreateProductUseCase {

    private final MenuCategoryPort categories;
    private final ProductPort products;
    private final ProductStatusHistoryPort history;
    private final Clock clock;

    public CreateProductService(MenuCategoryPort categories, ProductPort products, ProductStatusHistoryPort history,
                                Clock clock) {
        this.categories = categories;
        this.products = products;
        this.history = history;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProductSummary create(EstablishmentId establishmentId, ProductForm form) {
        // A categoria precisa ser deste restaurante e estar ativa: um id de outro restaurante dá 404.
        MenuCategory category = MenuLookups.activeCategory(categories, establishmentId, form.categoryId());
        if (products.countActive(establishmentId) >= Product.MAX_PER_ESTABLISHMENT) {
            throw new MenuFullException();
        }
        Product product = Product.create(establishmentId, category.id(), form.details(), form.available(),
                products.nextPosition(establishmentId, category.id()));
        products.save(product);
        // O ponto de partida do histórico (promoção, destaque e "disponível") dos relatórios.
        history.record(ProductStatusChange.between(null, product, clock.instant()));
        return ProductSummary.from(product);
    }
}
