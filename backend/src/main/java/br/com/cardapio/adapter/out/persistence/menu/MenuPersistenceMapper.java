package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.application.port.out.menu.MenuCategoryOverview;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.model.Money;

final class MenuPersistenceMapper {

    private MenuPersistenceMapper() {
    }

    static MenuCategoryJpaEntity toEntity(MenuCategory c) {
        return new MenuCategoryJpaEntity(c.id().value(), c.establishmentId().value(), c.name(), c.position(),
                c.active());
    }

    static MenuCategory toDomain(MenuCategoryJpaEntity e) {
        return new MenuCategory(new MenuCategoryId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                e.getName(), e.getPosition(), e.isActive());
    }

    static MenuCategoryOverview toDomain(MenuCategoryRow row) {
        MenuCategory category = new MenuCategory(new MenuCategoryId(row.getId()),
                new EstablishmentId(row.getEstablishmentId()), row.getName(), row.getPosition(), row.getActive());
        return new MenuCategoryOverview(category, row.getActiveProducts());
    }

    static ProductJpaEntity toEntity(Product p) {
        ProductDetails d = p.details();
        return new ProductJpaEntity(p.id().value(), p.establishmentId().value(), p.categoryId().value(), d.name(),
                d.description(), d.price().cents(), d.onSale() ? d.promoPrice().cents() : null, d.featured(),
                p.available(), p.position(), p.active());
    }

    static Product toDomain(ProductJpaEntity e) {
        ProductDetails details = new ProductDetails(e.getName(), e.getDescription(), Money.ofCents(e.getPriceCents()),
                e.getPromoPriceCents() == null ? null : Money.ofCents(e.getPromoPriceCents()), e.isFeatured());
        return new Product(new ProductId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new MenuCategoryId(e.getCategoryId()), details, e.isAvailable(), e.getPosition(), e.isActive());
    }
}
