package br.com.cardapio.domain.menu.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.exception.ChoiceUnavailableException;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O preço do item com o tamanho e as opções (ADR 0010), em cada modo de cobrança. */
class ProductOptionsTest {

    private static final EstablishmentId SAKURA = EstablishmentId.newId();
    private static final ProductId PIZZA = ProductId.newId();

    private static Money brl(long cents) {
        return Money.ofCents(cents);
    }

    private static ProductDetails product(String name, long cents) {
        return new ProductDetails(name, "", brl(cents), null, false);
    }

    private static ProductVariant variant(String name, long cents, int position) {
        return new ProductVariant(ProductVariantId.newId(), SAKURA, PIZZA, name, brl(cents), null, true, false,
                position, true);
    }

    private static OptionGroup group(String title, OptionGroupRules rules) {
        return OptionGroup.create(SAKURA, null, title, rules);
    }

    private static OptionItem option(OptionGroup group, String name, long cents, int position) {
        return new OptionItem(OptionItemId.newId(), SAKURA, group.id(), name, brl(cents), true, position, true);
    }

    private static OptionSelection pick(ProductVariant variant, OptionItem... options) {
        return new OptionSelection(variant == null ? null : variant.id(),
                Arrays.stream(options).map(o -> new SelectedOption(o.id(), 1)).toList());
    }

    private static OptionSelection pickTimes(ProductVariant variant, SelectedOption... options) {
        return new OptionSelection(variant == null ? null : variant.id(), List.of(options));
    }

    @Test
    void theUsualProductKeepsItsPriceAndAcceptsNoChoice() {
        ProductOptions none = ProductOptions.none(PIZZA);
        ProductDetails guioza = new ProductDetails("Guioza", "", brl(2990), brl(2490), false);

        PricedSelection priced = none.price(guioza, OptionSelection.NONE, false);

        assertThat(none.isEmpty()).isTrue();
        assertThat(priced.unitPrice()).isEqualTo(brl(2490));
        assertThat(priced.choices().isEmpty()).isTrue();
        assertThatThrownBy(() -> none.price(guioza, new OptionSelection(ProductVariantId.newId(), List.of()), false))
                .isInstanceOf(ChoiceUnavailableException.class);
    }

    @Nested
    class HalfAndHalfPizza {

        // A pizza: o preço vem do sabor (preço base zero), pelo sabor mais caro, com preço por tamanho.
        final ProductVariant broto = variant("Broto", 0, 0);
        final ProductVariant grande = variant("Grande", 0, 1);
        final OptionGroup flavors = group("Escolha os sabores",
                new OptionGroupRules(1, 2, 1, true, OptionPricing.HIGHEST, 0));
        final OptionItem calabresa = option(flavors, "Calabresa", 3990, 0);
        final OptionItem frango = option(flavors, "Frango com catupiry", 4490, 1);
        final OptionItem portuguesa = option(flavors, "Portuguesa", 4290, 2);
        final OptionGroup crust = group("Borda", new OptionGroupRules(0, 1, 1, false, OptionPricing.SUM, 0));
        final OptionItem catupiry = option(crust, "Catupiry", 800, 0);
        final ProductOptions pizza = new ProductOptions(PIZZA, List.of(broto, grande), List.of(
                new ProductOptions.LinkedGroup(flavors, List.of(calabresa, frango, portuguesa),
                        Map.of(broto.id(), new ChoiceLimit(1, 1), grande.id(), new ChoiceLimit(1, 3))),
                new ProductOptions.LinkedGroup(crust, List.of(catupiry), Map.of())),
                Map.of(new ProductOptions.PriceKey(calabresa.id(), grande.id()), brl(5490),
                        new ProductOptions.PriceKey(frango.id(), grande.id()), brl(5990),
                        new ProductOptions.PriceKey(catupiry.id(), grande.id()), brl(1200)));
        final ProductDetails details = product("Pizza", 0);

        @Test
        void chargesTheMostExpensiveFlavorAtTheSizePrice() {
            PricedSelection priced = pizza.price(details, pick(grande, calabresa, frango), false);

            assertThat(priced.unitPrice()).isEqualTo(brl(5990));
            assertThat(priced.choices().variantName()).isEqualTo("Grande");
            assertThat(priced.choices().options()).extracting(ChosenOption::name)
                    .containsExactly("Calabresa", "Frango com catupiry");
            assertThat(priced.choices().options()).allMatch(ChosenOption::splitsItem);
            assertThat(priced.choices().options()).extracting(ChosenOption::charged)
                    .containsExactly(Money.ZERO, brl(5990));
        }

        @Test
        void theCrustAddsItsSizePrice() {
            PricedSelection priced = pizza.price(details, pick(grande, calabresa, catupiry), false);

            assertThat(priced.unitPrice()).isEqualTo(brl(5490 + 1200));
        }

        @Test
        void eachSizeHasItsOwnNumberOfFlavors() {
            assertThatThrownBy(() -> pizza.price(details, pick(broto, calabresa, frango), false))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessage("Pizza · Escolha os sabores: escolha 1.");
            assertThat(pizza.price(details, pick(grande, calabresa, frango, portuguesa), false).unitPrice())
                    .isEqualTo(brl(5990));
            // Sem preço próprio no tamanho, vale o preço da opção.
            assertThat(pizza.price(details, pick(broto, portuguesa), false).unitPrice()).isEqualTo(brl(4290));
        }

        @Test
        void theSizeAndTheFlavorsAreRequired() {
            assertThatThrownBy(() -> pizza.price(details, pick(null, calabresa), false))
                    .isInstanceOf(InvalidValueException.class).hasMessage("Pizza: escolha o tamanho.");
            assertThatThrownBy(() -> pizza.price(details, pick(grande), false))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessage("Pizza · Escolha os sabores: escolha de 1 a 3.");
        }

        @Test
        void aSoldOutFlavorOrSizeCannotBeChosen() {
            ProductOptions soldOut = new ProductOptions(PIZZA, List.of(broto, grande.withAvailability(false)),
                    List.of(new ProductOptions.LinkedGroup(flavors,
                            List.of(calabresa, frango.withAvailability(false)), Map.of())), Map.of());

            assertThatThrownBy(() -> soldOut.price(details, pick(broto, frango), false))
                    .isInstanceOf(ChoiceUnavailableException.class)
                    .hasMessage("Pizza: Frango com catupiry acabou. Escolha outra opção.");
            assertThatThrownBy(() -> soldOut.price(details, pick(grande, calabresa), false))
                    .isInstanceOf(ChoiceUnavailableException.class)
                    .hasMessageContaining("o tamanho Grande acabou");
        }

        @Test
        void anOptionOutOfTheMenuIsRefused() {
            OptionItem removed = option(flavors, "Atum", 4000, 9);

            assertThatThrownBy(() -> pizza.price(details, pick(grande, removed), false))
                    .isInstanceOf(ChoiceUnavailableException.class)
                    .hasMessage("Uma escolha de Pizza não está mais no cardápio. Escolha de novo.");
        }

        @Test
        void fromPriceIsTheCheapestSizeWithTheCheapestFlavor() {
            assertThat(pizza.fromPrice(details)).isEqualTo(brl(3990));
        }
    }

    @Nested
    class Burger {

        final OptionGroup doneness = group("Ponto da carne",
                new OptionGroupRules(1, 1, 1, false, OptionPricing.SUM, 0));
        final OptionItem medium = option(doneness, "Ao ponto", 0, 0);
        final OptionGroup extras = group("Adicionais", new OptionGroupRules(0, 5, 3, false, OptionPricing.SUM, 0));
        final OptionItem bacon = option(extras, "Bacon", 500, 0);
        final OptionItem cheddar = option(extras, "Cheddar", 400, 1);
        final ProductOptions burger = new ProductOptions(PIZZA, List.of(), List.of(
                new ProductOptions.LinkedGroup(doneness, List.of(medium), Map.of()),
                new ProductOptions.LinkedGroup(extras, List.of(bacon, cheddar), Map.of())), Map.of());
        final ProductDetails details = new ProductDetails("Smash", "", brl(3200), brl(2900), false);

        @Test
        void extrasAddUpOnThePromotionalBasePriceAndCanRepeat() {
            PricedSelection priced = burger.price(details, pickTimes(null, new SelectedOption(medium.id(), 1),
                    new SelectedOption(bacon.id(), 2), new SelectedOption(cheddar.id(), 1)), false);

            assertThat(priced.unitPrice()).isEqualTo(brl(2900 + 2 * 500 + 400));
            assertThat(priced.choices().variantId()).isNull();
            assertThat(priced.choices().options()).extracting(ChosenOption::quantity).containsExactly(1, 2, 1);
        }

        @Test
        void theSameOptionSentTwiceCountsTogether() {
            PricedSelection priced = burger.price(details, pickTimes(null, new SelectedOption(medium.id(), 1),
                    new SelectedOption(bacon.id(), 1), new SelectedOption(bacon.id(), 1)), false);

            assertThat(priced.unitPrice()).isEqualTo(brl(2900 + 1000));
        }

        @Test
        void eachOptionHasItsOwnMaximum() {
            assertThatThrownBy(() -> burger.price(details, pickTimes(null, new SelectedOption(medium.id(), 1),
                    new SelectedOption(bacon.id(), 4)), false))
                    .isInstanceOf(InvalidValueException.class).hasMessage("Smash · Bacon: até 3 vezes.");
        }

        @Test
        void fromPriceIsNotRaisedByOptionalExtras() {
            assertThat(burger.fromPrice(details)).isEqualTo(brl(2900));
        }

        @Test
        void inTheRodizioTheItemIsFreeButPaidExtrasStayOut() {
            PricedSelection round = burger.price(details, pick(null, medium), true);

            assertThat(round.unitPrice()).isEqualTo(Money.ZERO);
            assertThat(round.choices().options()).extracting(ChosenOption::name).containsExactly("Ao ponto");
            assertThatThrownBy(() -> burger.price(details, pick(null, medium, bacon), true))
                    .isInstanceOf(InvalidValueException.class).hasMessageContaining("não entra no rodízio");
        }
    }

    @Test
    void acaiFreeToppingsAreTheCheapestOnes() {
        OptionGroup toppings = group("Complementos", new OptionGroupRules(0, 8, 1, false, OptionPricing.SUM, 3));
        OptionItem granola = option(toppings, "Granola", 200, 0);
        OptionItem banana = option(toppings, "Banana", 200, 1);
        OptionItem leite = option(toppings, "Leite em pó", 300, 2);
        OptionItem nutella = option(toppings, "Nutella", 600, 3);
        ProductVariant small = variant("300 ml", 1800, 0);
        ProductOptions acai = new ProductOptions(PIZZA, List.of(small),
                List.of(new ProductOptions.LinkedGroup(toppings, List.of(granola, banana, leite, nutella), Map.of())),
                Map.of());

        PricedSelection priced = acai.price(product("Açaí", 0), pick(small, granola, banana, leite, nutella), false);

        // Os três mais baratos ficam grátis; paga só a Nutella.
        assertThat(priced.unitPrice()).isEqualTo(brl(1800 + 600));
        assertThat(priced.choices().options()).extracting(ChosenOption::charged)
                .containsExactly(Money.ZERO, Money.ZERO, Money.ZERO, brl(600));
    }

    @Test
    void theAverageIsRoundedUpToTheCent() {
        OptionGroup flavors = group("Sabores", new OptionGroupRules(1, 3, 1, true, OptionPricing.AVERAGE, 0));
        OptionItem a = option(flavors, "A", 1000, 0);
        OptionItem b = option(flavors, "B", 1000, 1);
        OptionItem c = option(flavors, "C", 1001, 2);
        ProductOptions three = new ProductOptions(PIZZA, List.of(),
                List.of(new ProductOptions.LinkedGroup(flavors, List.of(a, b, c), Map.of())), Map.of());

        PricedSelection priced = three.price(product("Pizza", 0), pick(null, a, b, c), false);

        // (1000 + 1000 + 1001) / 3 = 1000,33 → 1001; as partes somam o total.
        assertThat(priced.unitPrice()).isEqualTo(brl(1001));
        assertThat(priced.choices().options().stream().map(ChosenOption::charged).reduce(Money.ZERO, Money::plus))
                .isEqualTo(brl(1001));
    }

    @Test
    void theRodizioUsesItsSizeWhateverThePhoneSends() {
        ProductVariant broto = variant("Broto", 3000, 0);
        ProductVariant media = new ProductVariant(ProductVariantId.newId(), SAKURA, PIZZA, "Média", brl(4000), null,
                true, true, 1, true);
        ProductOptions sizes = new ProductOptions(PIZZA, List.of(broto, media), List.of(), Map.of());

        PricedSelection round = sizes.price(product("Pizza", 0), pick(broto), true);

        assertThat(round.unitPrice()).isEqualTo(Money.ZERO);
        assertThat(round.choices().variantName()).isEqualTo("Média");
    }

    @Test
    void inactiveSizesGroupsAndOptionsAreLeftOut() {
        OptionGroup gone = group("Antigo", new OptionGroupRules(1, 1, 1, false, OptionPricing.SUM, 0)).deactivate();
        ProductVariant old = variant("Família", 9000, 0).deactivate();
        ProductOptions options = new ProductOptions(PIZZA, List.of(old),
                List.of(new ProductOptions.LinkedGroup(gone, List.of(option(gone, "X", 0, 0)), Map.of())), Map.of());

        assertThat(options.isEmpty()).isTrue();
        assertThat(options.price(product("Pizza", 4500), OptionSelection.NONE, false).unitPrice())
                .isEqualTo(brl(4500));
    }
}
