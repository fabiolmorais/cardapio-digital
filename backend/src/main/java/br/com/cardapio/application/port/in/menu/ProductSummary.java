package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;

public record ProductSummary(
        ProductId id,
        MenuCategoryId categoryId,
        String name,
        String description,
        long priceCents,
        Long promoPriceCents,
        boolean featured,
        boolean available,
        int position
) {

    public static ProductSummary from(Product product) {
        var details = product.details();
        return new ProductSummary(product.id(), product.categoryId(), details.name(), details.description(),
                details.price().cents(), details.onSale() ? details.promoPrice().cents() : null,
                details.featured(), product.available(), product.position());
    }
}
