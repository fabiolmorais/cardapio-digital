package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.out.menu.MenuCategoryOverview;
import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.*;
import br.com.cardapio.domain.shared.model.Money;

import java.util.*;

/** Categorias e produtos em memória. Conta quantas vezes cada produto foi gravado. */
final class MenuFakes implements MenuCategoryPort, ProductPort {

    final Map<MenuCategoryId, MenuCategory> categories = new LinkedHashMap<>();
    final Map<ProductId, Product> products = new LinkedHashMap<>();
    int productSaves;

    MenuCategory givenCategory(EstablishmentId establishmentId, String name) {
        MenuCategory category = MenuCategory.create(establishmentId, name, nextPosition(establishmentId));
        categories.put(category.id(), category);
        return category;
    }

    Product givenProduct(MenuCategory category, String name, long priceCents) {
        Product product = Product.create(category.establishmentId(), category.id(),
                new ProductDetails(name, "", Money.ofCents(priceCents), null, false), true,
                nextPosition(category.establishmentId(), category.id()));
        products.put(product.id(), product);
        return product;
    }

    // MenuCategoryPort
    @Override
    public void save(MenuCategory category) { categories.put(category.id(), category); }

    @Override
    public Optional<MenuCategory> find(EstablishmentId establishmentId, MenuCategoryId id) {
        return Optional.ofNullable(categories.get(id)).filter(c -> c.establishmentId().equals(establishmentId));
    }

    @Override
    public ResultPage<MenuCategoryOverview> findOverview(EstablishmentId establishmentId, PageQuery page) {
        List<MenuCategoryOverview> all = findActive(establishmentId).stream()
                .map(c -> new MenuCategoryOverview(c, countActiveProducts(establishmentId, c.id())))
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public List<MenuCategory> findActive(EstablishmentId establishmentId) {
        return categories.values().stream()
                .filter(c -> c.establishmentId().equals(establishmentId) && c.active())
                .sorted(Comparator.comparingInt(MenuCategory::position))
                .toList();
    }

    @Override
    public boolean activeNameExists(EstablishmentId establishmentId, String name, MenuCategoryId exceptId) {
        return categories.values().stream().anyMatch(c -> c.establishmentId().equals(establishmentId) && c.active()
                && c.name().equalsIgnoreCase(name) && !c.id().equals(exceptId));
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId) {
        return categories.values().stream().filter(c -> c.establishmentId().equals(establishmentId))
                .mapToInt(c -> c.position() + 1).max().orElse(0);
    }

    @Override
    public long countActiveProducts(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        return findActiveByCategory(establishmentId, categoryId).size();
    }

    // ProductPort
    @Override
    public void save(Product product) {
        products.put(product.id(), product);
        productSaves++;
    }

    @Override
    public Optional<Product> find(EstablishmentId establishmentId, ProductId id) {
        return Optional.ofNullable(products.get(id)).filter(p -> p.establishmentId().equals(establishmentId));
    }

    @Override
    public ResultPage<Product> search(EstablishmentId establishmentId, MenuCategoryId categoryId, String text,
                                      PageQuery page) {
        Comparator<Product> menuOrder = Comparator
                .comparingInt((Product p) -> categories.get(p.categoryId()).position())
                .thenComparingInt(Product::position);
        List<Product> all = products.values().stream()
                .filter(p -> p.establishmentId().equals(establishmentId) && p.active())
                .filter(p -> categoryId == null || p.categoryId().equals(categoryId))
                .filter(p -> text == null || p.details().name().toLowerCase(Locale.ROOT)
                        .contains(text.toLowerCase(Locale.ROOT)))
                .sorted(menuOrder)
                .toList();
        return new ResultPage<>(all, page.page(), page.size(), all.size());
    }

    @Override
    public List<Product> findActiveByCategory(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        return products.values().stream()
                .filter(p -> p.establishmentId().equals(establishmentId) && p.categoryId().equals(categoryId)
                        && p.active())
                .sorted(Comparator.comparingInt(Product::position))
                .toList();
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        return products.values().stream()
                .filter(p -> p.establishmentId().equals(establishmentId) && p.categoryId().equals(categoryId))
                .mapToInt(p -> p.position() + 1).max().orElse(0);
    }
}
