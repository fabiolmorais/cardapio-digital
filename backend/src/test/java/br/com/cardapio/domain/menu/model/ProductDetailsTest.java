package br.com.cardapio.domain.menu.model;


import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductDetailsTest {

    private static ProductDetails details(long price, Long promo) {
        return new ProductDetails("Sunomono", null, Money.ofCents(price),
                promo == null ? null : Money.ofCents(promo), false);
    }

    @Test
    void withoutPromotionTheClientPaysThePrice() {
        ProductDetails sunomono = details(1990, null);

        assertThat(sunomono.onSale()).isFalse();
        assertThat(sunomono.currentPrice()).isEqualTo(Money.ofCents(1990));
        assertThat(sunomono.description()).isEmpty();
    }

    @Test
    void onSaleTheClientPaysThePromotionalPrice() {
        ProductDetails sunomono = details(1990, 1590L);

        assertThat(sunomono.onSale()).isTrue();
        assertThat(sunomono.currentPrice()).isEqualTo(Money.ofCents(1590));
    }

    @Test
    void thePromotionalPriceMustBeLowerThanThePriceAndAboveZero() {
        assertThatThrownBy(() -> details(1990, 1990L))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("O preço promocional precisa ser menor que o preço normal");
        assertThatThrownBy(() -> details(1990, 2500L)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> details(1990, 0L))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("O preço promocional precisa ser maior que zero");
    }

    @Test
    void freeIsAllowedButAbsurdPricesAreNot() {
        assertThat(details(0, null).price()).isEqualTo(Money.ZERO);
        assertThat(details(9_999_999, null).price()).isEqualTo(ProductDetails.PRICE_MAX);
        assertThatThrownBy(() -> details(10_000_000, null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void nameAndDescriptionHaveLimits() {
        Money price = Money.ofCents(1000);
        assertThatThrownBy(() -> new ProductDetails(" ", "", price, null, false))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ProductDetails("x".repeat(81), "", price, null, false))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ProductDetails("Guioza", "x".repeat(301), price, null, false))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new ProductDetails("  Guioza ", "  6 unidades ", price, null, true))
                .extracting(ProductDetails::name, ProductDetails::description)
                .containsExactly("Guioza", "6 unidades");
    }
}