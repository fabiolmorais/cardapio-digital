package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.util.Objects;

/**
 * Produto do cardápio. "Disponível" é o interruptor do dia a dia (acabou o salmão: o cliente vê
 * "Esgotado hoje"); "ativo" é o cadastro (desativado, o produto sai do cardápio e do painel).
 */
public record Product(
        ProductId id,
        EstablishmentId establishmentId,
        MenuCategoryId categoryId,
        ProductDetails details,
        boolean available,
        int position,
        boolean active
) {

    public Product {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(details);
        if (position < 0) {
            throw new IllegalArgumentException("posição não pode ser negativa");
        }
    }

    /** Produto novo entra no fim da categoria. */
    public static Product create(EstablishmentId establishmentId, MenuCategoryId categoryId, ProductDetails details,
                                 boolean available, int position) {
        return new Product(ProductId.newId(), establishmentId, categoryId, details, available, position, true);
    }

    public Product edit(ProductDetails newDetails) {
        return new Product(id, establishmentId, categoryId, Objects.requireNonNull(newDetails), available, position,
                active);
    }

    /** Muda de categoria e vai para o fim dela (newPosition). */
    public Product moveTo(MenuCategoryId newCategory, int newPosition) {
        return new Product(id, establishmentId, Objects.requireNonNull(newCategory), details, available, newPosition,
                active);
    }

    /** Novo lugar dentro da categoria, depois de arrastar. */
    public Product at(int newPosition) {
        return new Product(id, establishmentId, categoryId, details, available, newPosition, active);
    }

    public Product markAvailable() {
        return new Product(id, establishmentId, categoryId, details, true, position, active);
    }

    public Product markSoldOut() {
        return new Product(id, establishmentId, categoryId, details, false, position, active);
    }

    public Product deactivate() {
        return new Product(id, establishmentId, categoryId, details, available, position, false);
    }
}
