package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/**
 * O faturamento das comandas que abriram num dia da semana e numa hora (no fuso do restaurante),
 * somado no período: a base do mapa de calor e do "por hora".
 *
 * @param isoWeekday 1 = segunda … 7 = domingo
 * @param hour       0 a 23
 */
public record HourlyRevenue(int isoWeekday, int hour, Money revenue, int tabs) {
}
