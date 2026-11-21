package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/** O faturamento médio de um dia da semana numa hora (1 = segunda … 7 = domingo; 0 a 23h). */
public record WeekdayHourAverage(int isoWeekday, int hour, Money average) {
}
