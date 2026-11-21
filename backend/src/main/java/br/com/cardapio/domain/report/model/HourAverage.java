package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.math.BigDecimal;

/** Uma hora do dia, em média por dia do período: quanto faturou e quantas comandas abriram. */
public record HourAverage(int hour, Money revenuePerDay, BigDecimal tabsPerDay) {
}
