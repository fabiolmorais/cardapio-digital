package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.GetStaffMenuUseCase;
import br.com.cardapio.application.port.in.menu.PublicMenu;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** O cardápio do garçom: o mesmo da mesa, sem mídia nenhuma. Três consultas, qualquer que seja o tamanho. */
public class GetStaffMenuService implements GetStaffMenuUseCase {

    private final MenuCategoryPort categories;
    private final ProductPort products;
    private final ProductOptionsPort options;

    public GetStaffMenuService(MenuCategoryPort categories, ProductPort products, ProductOptionsPort options) {
        this.categories = categories;
        this.products = products;
        this.options = options;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMenu get(EstablishmentId establishmentId) {
        Map<ProductId, ProductOptions> configured = options.findAllActive(establishmentId);
        Map<MenuCategoryId, List<PublicMenu.Item>> byCategory = products.findAllActive(establishmentId).stream()
                .collect(Collectors.groupingBy(Product::categoryId, Collectors.mapping(
                        p -> item(p, configured.getOrDefault(p.id(), ProductOptions.none(p.id()))),
                        Collectors.toList())));
        return new PublicMenu(categories.findActive(establishmentId).stream()
                .filter(c -> byCategory.containsKey(c.id()))
                .map(c -> new PublicMenu.Category(c.id(), c.name(), byCategory.get(c.id())))
                .toList(), List.of(), Map.of());
    }

    private static PublicMenu.Item item(Product product, ProductOptions choices) {
        ProductDetails d = product.details();
        return new PublicMenu.Item(product.id(), d.name(), d.description(), d.price().cents(),
                d.onSale() ? d.promoPrice().cents() : null, d.featured(), product.available(), d.aLaCarte(), d.serves(),
                Map.of(), List.of(), choices, GetPublicMenuService.fromPrice(product, choices));
    }
}
