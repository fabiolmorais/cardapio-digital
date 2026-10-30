package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.out.menu.MenuCategoryPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.MenuCategoryNotFoundException;
import br.com.cardapio.domain.menu.exception.ProductNotFoundException;
import br.com.cardapio.domain.menu.model.MenuCategory;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;

/**
 * As buscas que quase todo caso de uso do cardápio repete: "a categoria (ou o produto) existe,
 * é deste restaurante e está ativa?". Desativado conta como inexistente: 404.
 */
final class MenuLookups {

    private MenuLookups() {
    }

    static MenuCategory activeCategory(MenuCategoryPort categories, EstablishmentId establishmentId,
                                       MenuCategoryId id) {
        return categories.find(establishmentId, id)
                .filter(MenuCategory::active)
                .orElseThrow(MenuCategoryNotFoundException::new);
    }

    static Product activeProduct(ProductPort products, EstablishmentId establishmentId, ProductId id) {
        return products.find(establishmentId, id)
                .filter(Product::active)
                .orElseThrow(ProductNotFoundException::new);
    }
}
