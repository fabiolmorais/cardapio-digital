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
        int position,
        boolean aLaCarte,
        /** Fotos e vídeos da galeria: só a lista conta; nos outros casos, null. */
        Integer photoCount,
        Integer videoCount
) {

    public static ProductSummary from(Product product) {
        var details = product.details();
        return new ProductSummary(product.id(), product.categoryId(), details.name(), details.description(),
                details.price().cents(), details.onSale() ? details.promoPrice().cents() : null,
                details.featured(), product.available(), product.position(), details.aLaCarte(), null, null);
    }

    /** O mesmo produto, com a contagem da galeria (a lista do admin mostra "3 fotos · 1 vídeo"). */
    public ProductSummary withMedia(int photos, int videos) {
        return new ProductSummary(id, categoryId, name, description, priceCents, promoPriceCents, featured, available,
                position, aLaCarte, photos, videos);
    }
}
