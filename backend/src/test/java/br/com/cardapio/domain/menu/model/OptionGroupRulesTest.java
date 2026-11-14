package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** As regras de um grupo de opções e dos tamanhos (ADR 0010). */
class OptionGroupRulesTest {

    @Test
    void minimumAndMaximumMakeSense() {
        assertThatThrownBy(() -> new OptionGroupRules(2, 1, 1, false, OptionPricing.SUM, 0))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new OptionGroupRules(0, 0, 1, false, OptionPricing.SUM, 0))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new OptionGroupRules(0, 21, 1, false, OptionPricing.SUM, 0))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new OptionGroupRules(0, 2, 3, false, OptionPricing.SUM, 0))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new OptionGroupRules(1, 1, 1, false, OptionPricing.SUM, 0).required()).isTrue();
        assertThat(new OptionGroupRules(0, 5, 2, false, OptionPricing.SUM, 0).required()).isFalse();
    }

    @Test
    void aSplitItemHasUpToFourDifferentParts() {
        assertThatThrownBy(() -> new OptionGroupRules(1, 5, 1, true, OptionPricing.HIGHEST, 0))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("até 4 partes");
        assertThatThrownBy(() -> new OptionGroupRules(1, 2, 2, true, OptionPricing.HIGHEST, 0))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new OptionGroupRules(1, 4, 1, true, OptionPricing.AVERAGE, 0).splitsItem()).isTrue();
    }

    @Test
    void freeChoicesOnlyWhenTheGroupAddsUp() {
        assertThatThrownBy(() -> new OptionGroupRules(0, 3, 1, true, OptionPricing.HIGHEST, 1))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("soma");
        assertThat(new OptionGroupRules(0, 8, 1, false, OptionPricing.SUM, 3).freeQuantity()).isEqualTo(3);
    }

    @Test
    void aSizeLimitReplacesTheGroupLimits() {
        OptionGroupRules flavors = new OptionGroupRules(1, 3, 1, true, OptionPricing.HIGHEST, 0);

        OptionGroupRules broto = flavors.limitedTo(new ChoiceLimit(1, 1));

        assertThat(broto.maxChoices()).isEqualTo(1);
        assertThat(broto.splitsItem()).isTrue();
        assertThat(flavors.limitedTo(null)).isEqualTo(flavors);
    }

    @Test
    void theGroupNeedsAQuestionAndUsesItAsTheNameWhenThereIsNone() {
        EstablishmentId sakura = EstablishmentId.newId();
        OptionGroupRules rules = new OptionGroupRules(1, 1, 1, false, OptionPricing.SUM, 0);

        assertThat(OptionGroup.create(sakura, " ", " Ponto da carne ", rules).name()).isEqualTo("Ponto da carne");
        assertThatThrownBy(() -> OptionGroup.create(sakura, null, "", rules)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aSizePromotionIsBelowItsPrice() {
        EstablishmentId sakura = EstablishmentId.newId();
        assertThatThrownBy(() -> new ProductVariant(ProductVariantId.newId(), sakura, ProductId.newId(), "Grande",
                Money.ofCents(5000), Money.ofCents(5000), true, false, 0, true))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ProductVariant(ProductVariantId.newId(), sakura, ProductId.newId(), "",
                Money.ofCents(5000), null, true, false, 0, true))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new ProductVariant(ProductVariantId.newId(), sakura, ProductId.newId(), "Grande",
                Money.ofCents(5000), Money.ofCents(4500), true, false, 0, true).currentPrice())
                .isEqualTo(Money.ofCents(4500));
    }
}
