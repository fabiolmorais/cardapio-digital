package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Um ponto da evolução de uma unidade: um dia, uma semana ou um mês, com o faturamento e as comandas
 * por dia em média (a semana cortada na ponta do período não parece queda).
 */
public record UnitPoint(LocalDate start, LocalDate end, Money revenuePerDay, BigDecimal tabsPerDay) {

    public UnitPoint {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(revenuePerDay);
        Objects.requireNonNull(tabsPerDay);
    }
}
