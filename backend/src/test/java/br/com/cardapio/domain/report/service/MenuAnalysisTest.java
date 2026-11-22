package br.com.cardapio.domain.report.service;

import java.util.UUID;
import java.util.List;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.report.model.VariantSales;
import br.com.cardapio.domain.report.model.ProductPair;
import br.com.cardapio.domain.report.model.OptionSales;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.AbcClass;
import br.com.cardapio.domain.menu.model.ProductId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MenuAnalysisTest {

    @Test
    void onlyProductsOlderThanThirtyDaysAreSlowSellerCandidates() {
        assertThat(MenuAnalysis.slowSellerCutoff(Instant.parse("2026-11-21T15:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-22T15:00:00Z"));
        assertThat(MenuAnalysis.SLOW_SELLERS).isEqualTo(5);
    }

    @Test
    void theAbcCurveIsByWhatTheItemsAheadAlreadySum() {
        Money total = Money.ofCents(10000);
        assertThat(AbcClass.of(Money.ZERO, total)).isEqualTo(AbcClass.A);
        // Começa antes dos 80%: é A, mesmo que passe deles.
        assertThat(AbcClass.of(Money.ofCents(7999), total)).isEqualTo(AbcClass.A);
        assertThat(AbcClass.of(Money.ofCents(8000), total)).isEqualTo(AbcClass.B);
        assertThat(AbcClass.of(Money.ofCents(9499), total)).isEqualTo(AbcClass.B);
        assertThat(AbcClass.of(Money.ofCents(9500), total)).isEqualTo(AbcClass.C);
        assertThat(AbcClass.of(Money.ZERO, Money.ZERO)).isEqualTo(AbcClass.C);
    }

    @Test
    void theSizesNeedTwoSoldAndComeFromTheMostChosen() {
        ProductId chopp = new ProductId(UUID.randomUUID());
        ProductId suco = new ProductId(UUID.randomUUID());
        var sizes = MenuAnalysis.sizes(List.of(new VariantSales(chopp, "Chopp", "300 ml", 151),
                new VariantSales(chopp, "Chopp", "500 ml", 247), new VariantSales(suco, "Suco", "500 ml", 90)), 3);

        assertThat(sizes).hasSize(1);
        assertThat(sizes.getFirst()).extracting(VariantSales::variant).containsExactly("500 ml", "300 ml");
    }

    @Test
    void theOptionsAndThePaidTotal() {
        var options = List.of(new OptionSales("Molhos", "Molho tarê", 88, Money.ofCents(26400), true),
                new OptionSales("Extras", "Cream cheese", 212, Money.ofCents(106000), true),
                new OptionSales("Extras", "Gengibre", 96, Money.ZERO, true));

        assertThat(MenuAnalysis.topOptions(options, 2)).extracting(OptionSales::option)
                .containsExactly("Cream cheese", "Gengibre");
        assertThat(MenuAnalysis.paidOptions(options)).isEqualTo(Money.ofCents(132400));
    }

    @Test
    void thePairsAreReadFromTheLeastOrderedAndNeedThreeTabs() {
        var pairs = MenuAnalysis.pairs(List.of(new ProductPair("Chopp", "Guioza", 300, 120, 50),
                new ProductPair("Temaki", "Hot roll", 80, 90, 27), new ProductPair("Chá", "Pastel", 5, 5, 2)), 3);

        assertThat(pairs).hasSize(2);
        assertThat(pairs.getFirst().first()).isEqualTo("Guioza");
        assertThat(pairs.getFirst().share()).isCloseTo(50.0 / 120, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void theMostCanceledListsEvenOneCancellation() {
        var list = MenuAnalysis.mostCanceled(List.of(new CanceledProductCount("Ceviche", "Cliente desistiu", 4),
                new CanceledProductCount("Ceviche", null, 2), new CanceledProductCount("Chá", null, 1)), 3);

        assertThat(list).extracting(AttentionPoint.CanceledProduct::name).containsExactly("Ceviche", "Chá");
        assertThat(list.getFirst().reason()).isEqualTo("Cliente desistiu");
    }
}
