package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderSettingsTest {

    private static final Instant NOW = Instant.parse("2026-11-03T20:00:00Z");
    private final OrderSettings defaults = OrderSettings.defaults(EstablishmentId.newId(), NOW);

    @Test
    void aNewRestaurantTakesOrdersStraightToTheKitchenWithLimits() {
        assertThat(defaults.orderingEnabled()).isTrue();
        assertThat(defaults.approvalRequired()).isFalse();
        assertThat(defaults.maxAmount()).isEqualTo(Money.ofCents(300_00));
        assertThat(defaults.maxItems()).isEqualTo(10);
    }

    @Test
    void anOrderAboveALimitWaitsForTheWaiter() {
        assertThat(defaults.needsApproval(Money.ofCents(300_00), 10)).isFalse();
        assertThat(defaults.needsApproval(Money.ofCents(300_01), 1)).isTrue();
        assertThat(defaults.needsApproval(Money.ofCents(10_00), 11)).isTrue();
    }

    @Test
    void withoutLimitsOnlyTheSwitchMatters() {
        OrderSettings noLimits = defaults.update(true, false, null, null, NOW);
        OrderSettings always = defaults.update(true, true, null, null, NOW);

        assertThat(noLimits.needsApproval(Money.ofCents(999_999_00), 99)).isFalse();
        assertThat(always.needsApproval(Money.ofCents(1_00), 1)).isTrue();
    }

    @Test
    void limitsMustMakeSense() {
        assertThatThrownBy(() -> defaults.update(true, false, Money.ofCents(50), null, NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> defaults.update(true, false, null, 0, NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> defaults.update(true, false, null, 101, NOW))
                .isInstanceOf(InvalidValueException.class);
    }
}
