package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.in.menu.ListProductsUseCase;
import br.com.cardapio.application.port.in.menu.ProductSummary;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import org.springframework.transaction.annotation.Transactional;

public class ListProductsService implements ListProductsUseCase {

    private final ProductPort products;

    public ListProductsService(ProductPort products) {
        this.products = products;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<ProductSummary> list(EstablishmentId establishmentId, MenuCategoryId categoryId, String text,
                                           PageQuery page) {
        String search = text == null || text.isBlank() ? null : text.trim();
        return products.search(establishmentId, categoryId, search, page).map(ProductSummary::from);
    }
}
