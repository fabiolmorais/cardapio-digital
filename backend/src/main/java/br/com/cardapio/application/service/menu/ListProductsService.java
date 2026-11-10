package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ListProductsUseCase;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.out.menu.MediaCount;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import org.springframework.transaction.annotation.Transactional;

public class ListProductsService implements ListProductsUseCase {

    private final ProductPort products;
    private final ProductMediaPort media;

    public ListProductsService(ProductPort products, ProductMediaPort media) {
        this.products = products;
        this.media = media;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<ProductSummary> list(EstablishmentId establishmentId, MenuCategoryId categoryId, String text,
                                           PageQuery page) {
        String search = text == null || text.isBlank() ? null : text.trim();
        ResultPage<Product> found = products.search(establishmentId, categoryId, search, page);
        var counts = media.countByProducts(establishmentId, found.content().stream().map(Product::id).toList());
        return found.map(p -> {
            MediaCount count = counts.getOrDefault(p.id(), MediaCount.NONE);
            return ProductSummary.from(p).withMedia(count.photos(), count.videos());
        });
    }
}
