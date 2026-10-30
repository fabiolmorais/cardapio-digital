package br.com.cardapio.domain.menu.model;


import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private final EstablishmentId sakura = EstablishmentId.newId();
    private final MenuCategoryId entradas = MenuCategoryId.newId();
    private final ProductDetails guioza = new ProductDetails("Guioza", "", Money.ofCents(2990), null, true);

    @Test
    void aNewProductIsActive() {
        Product product = Product.create(sakura, entradas, guioza, true, 4);

        assertThat(product.active()).isTrue();
        assertThat(product.available()).isTrue();
        assertThat(product.position()).isEqualTo(4);
    }

    @Test
    void soldOutIsNotTheSameAsDeactivated() {
        Product product = Product.create(sakura, entradas, guioza, true, 0);

        Product soldOut = product.markSoldOut();
        assertThat(soldOut.available()).isFalse();
        assertThat(soldOut.active()).isTrue();

        Product gone = soldOut.markAvailable().deactivate();
        assertThat(gone.available()).isTrue();
        assertThat(gone.active()).isFalse();
    }

    @Test
    void editAndMoveKeepTheIdentity() {
        Product product = Product.create(sakura, entradas, guioza, true, 0);
        MenuCategoryId quentes = MenuCategoryId.newId();
        ProductDetails onSale = new ProductDetails("Guioza (6 un.)", "", Money.ofCents(2990), Money.ofCents(2490),
                true);

        Product changed = product.edit(onSale).moveTo(quentes, 7);

        assertThat(changed.id()).isEqualTo(product.id());
        assertThat(changed.categoryId()).isEqualTo(quentes);
        assertThat(changed.position()).isEqualTo(7);
        assertThat(changed.details().currentPrice()).isEqualTo(Money.ofCents(2490));
    }
}