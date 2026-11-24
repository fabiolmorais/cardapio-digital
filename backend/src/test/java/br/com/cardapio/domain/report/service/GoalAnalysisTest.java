package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.GoalProgress;
import br.com.cardapio.domain.report.model.RevenueGoal;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoalAnalysisTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final RevenueGoal GOAL = new RevenueGoal(NOVEMBER, Money.ofCents(18000000));

    @Test
    void whatIsMissingIsSplitByTheDaysLeftRoundingUp() {
        GoalProgress p = GoalAnalysis.progress(GOAL, Money.ofCents(12430000), 17, Money.ofCents(12430000), 13);

        // Faltam R$ 55.700,00 em 17 dias: R$ 3.276,4705… por dia, arredondado para cima.
        assertThat(p.perDayNeeded()).contains(Money.ofCents(327648));
        assertThat(p.dailyAverage()).contains(Money.ofCents(956154));
        assertThat(Math.round(p.share() * 100)).isEqualTo(69);
        assertThat(p.reached()).isFalse();
    }

    @Test
    void aReachedGoalNeedsNothingMore() {
        GoalProgress p = GoalAnalysis.progress(GOAL, Money.ofCents(18000000), 5, Money.ofCents(18000000), 25);

        assertThat(p.reached()).isTrue();
        assertThat(p.perDayNeeded()).isEmpty();
    }

    @Test
    void theFirstDayHasNoAverageAndTheLastDayNoDaysLeft() {
        assertThat(GoalAnalysis.progress(GOAL, Money.ZERO, 30, Money.ZERO, 0).dailyAverage()).isEmpty();
        assertThat(GoalAnalysis.progress(GOAL, Money.ofCents(100), 0, Money.ofCents(100), 30).perDayNeeded())
                .isEmpty();
    }

    @Test
    void theGoalHasARange() {
        assertThatThrownBy(() -> new RevenueGoal(NOVEMBER, Money.ZERO)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RevenueGoal(NOVEMBER, Money.ofCents(RevenueGoal.MAX_CENTS + 1)))
                .isInstanceOf(InvalidValueException.class);
    }
}
