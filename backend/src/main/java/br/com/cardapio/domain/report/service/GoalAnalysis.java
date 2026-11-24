package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.GoalProgress;
import br.com.cardapio.domain.report.model.RevenueGoal;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Optional;

/** As contas da meta de faturamento (REL-79). */
public final class GoalAnalysis {

    private GoalAnalysis() {
    }

    /**
     * @param achieved       o faturamento do mês até agora
     * @param daysLeft       os dias que ainda contam para a meta
     * @param closedRevenue  o faturamento dos dias já terminados do mês (para a média)
     * @param closedDays     quantos dias do mês já terminaram
     */
    public static GoalProgress progress(RevenueGoal goal, Money achieved, int daysLeft, Money closedRevenue,
                                        int closedDays) {
        long missing = goal.amount().cents() - achieved.cents();
        Optional<Money> perDay = missing > 0 && daysLeft > 0
                ? Optional.of(Money.ofCents((missing + daysLeft - 1) / daysLeft))
                : Optional.empty();
        Optional<Money> average = closedDays > 0
                ? Optional.of(Money.ofCents(Math.round((double) closedRevenue.cents() / closedDays)))
                : Optional.empty();
        return new GoalProgress(goal.month(), goal.amount(), achieved, Math.max(daysLeft, 0), perDay, average);
    }
}
