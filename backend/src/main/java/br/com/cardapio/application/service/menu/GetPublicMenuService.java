package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.GetPublicMenuUseCase;
import br.com.cardapio.application.port.in.menu.PublicMenu;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.service.floor.PublicTableLookup;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta o cardápio da página da mesa com três consultas, qualquer que seja o tamanho dele:
 * as categorias, todos os produtos e todas as capas. Juntar em memória é o que evita o N+1
 * (uma consulta de foto por produto, que com 200 produtos seriam 200 idas ao banco).
 */
public class GetPublicMenuService implements GetPublicMenuUseCase {

    private final DiningTablePort tables;
    private final MenuCategoryPort categories;
    private final ProductPort products;
    private final ProductMediaPort media;
    private final MediaStoragePort storage;

    public GetPublicMenuService(DiningTablePort tables, MenuCategoryPort categories, ProductPort products,
                                ProductMediaPort media, MediaStoragePort storage) {
        this.tables = tables;
        this.categories = categories;
        this.products = products;
        this.media = media;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMenu get(String tableToken) {
        EstablishmentId establishmentId = PublicTableLookup.resolve(tables, tableToken).table().establishmentId();

        Map<ProductId, ProductMedia> covers = media.findCovers(establishmentId).stream()
                .collect(Collectors.toMap(ProductMedia::productId, Function.identity()));
        Map<MenuCategoryId, List<PublicMenu.Item>> byCategory = products.findAllActive(establishmentId).stream()
                .collect(Collectors.groupingBy(Product::categoryId,
                        Collectors.mapping(p -> item(p, covers.get(p.id())), Collectors.toList())));

        List<PublicMenu.Category> visible = categories.findActive(establishmentId).stream()
                .filter(c -> byCategory.containsKey(c.id()))
                .map(c -> new PublicMenu.Category(c.id(), c.name(), byCategory.get(c.id())))
                .toList();
        return new PublicMenu(visible);
    }

    private PublicMenu.Item item(Product product, ProductMedia cover) {
        ProductDetails d = product.details();
        Map<ImageVariant, String> photo = cover == null ? Map.of()
                : Arrays.stream(ImageVariant.values())
                        .collect(Collectors.toMap(Function.identity(), v -> storage.publicUrl(cover.fileOf(v))));
        return new PublicMenu.Item(product.id(), d.name(), d.description(), d.price().cents(),
                d.onSale() ? d.promoPrice().cents() : null, d.featured(), product.available(), photo);
    }
}
