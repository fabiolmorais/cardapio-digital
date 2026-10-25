package br.com.cardapio.domain.plan.model;

import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanTest {

    private static Plan plan(Money monthly, Money annual, int historyDays, Set<PlanFeature> features) {
        return new Plan(new PlanId(UUID.randomUUID()), "pro", "Pro", monthly, annual,
                Limit.of(50), Limit.unlimited(), Limit.of(3), historyDays, features, SupportLevel.WHATSAPP, 2);
    }

    @Test
    void planWithoutPriceIsCustomPricing() {
        Plan enterprise = plan(null, null, 0, Set.of());
        assertThat(enterprise.isCustomPricing()).isTrue();
        assertThat(enterprise.monthly()).isEmpty();
    }

    @Test
    void pricedPlanNeedsBothPrices() {
        assertThatThrownBy(() -> plan(Money.ofCents(11990), null, 365, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroHistoryDaysMeansUnlimited() {
        assertThat(plan(null, null, 0, Set.of()).hasUnlimitedHistory()).isTrue();
        assertThat(plan(Money.ofCents(1), Money.ofCents(10), 90, Set.of()).hasUnlimitedHistory()).isFalse();
    }

    @Test
    void rejectsNegativeHistoryDays() {
        assertThatThrownBy(() -> plan(null, null, -1, Set.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checksFeaturesAndTableLimit() {
        Plan pro = plan(Money.ofCents(11990), Money.ofCents(119900), 365, Set.of(PlanFeature.PRODUCT_VIDEOS));
        assertThat(pro.includes(PlanFeature.PRODUCT_VIDEOS)).isTrue();
        assertThat(pro.includes(PlanFeature.MULTI_UNIT)).isFalse();
        assertThat(pro.allowsTables(50)).isTrue();
        assertThat(pro.allowsTables(51)).isFalse();
    }

    @Test
    void featuresAreImmutable() {
        Plan pro = plan(null, null, 0, Set.of(PlanFeature.PRODUCT_VIDEOS));
        assertThatThrownBy(() -> pro.features().add(PlanFeature.MULTI_UNIT))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresCodeAndName() {
        assertThatThrownBy(() -> new Plan(new PlanId(UUID.randomUUID()), " ", "Pro", null, null,
                Limit.unlimited(), Limit.unlimited(), Limit.unlimited(), 0, Set.of(), SupportLevel.EMAIL, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Plan(new PlanId(UUID.randomUUID()), "pro", "", null, null,
                Limit.unlimited(), Limit.unlimited(), Limit.unlimited(), 0, Set.of(), SupportLevel.EMAIL, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
