package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

/**
 * Como o mês está contra a meta (REL-79): "R$ 124.300 de R$ 180.000, 69%. Faltam 11 dias: R$ 5.060 por
 * dia batem a meta (a média está em R$ 6.890)".
 *
 * @param daysLeft     os dias que ainda contam (o de hoje, na Visão geral; no resumo, os depois do dia)
 * @param perDayNeeded quanto por dia, nos dias que faltam, bate a meta; vazio se já bateu ou o mês acabou
 * @param dailyAverage a média por dia dos dias já terminados do mês; vazio no primeiro dia
 */
public record GoalProgress(YearMonth month, Money goal, Money achieved, int daysLeft,
                           Optional<Money> perDayNeeded, Optional<Money> dailyAverage) {

    public GoalProgress {
        Objects.requireNonNull(month, "mês é obrigatório");
        Objects.requireNonNull(goal, "meta é obrigatória");
        Objects.requireNonNull(achieved, "faturamento é obrigatório");
        Objects.requireNonNull(perDayNeeded, "por dia é obrigatório");
        Objects.requireNonNull(dailyAverage, "média é obrigatória");
    }

    /** A fração da meta já faturada (pode passar de 1). */
    public double share() {
        return (double) achieved.cents() / goal.cents();
    }

    public boolean reached() {
        return achieved.cents() >= goal.cents();
    }
}
