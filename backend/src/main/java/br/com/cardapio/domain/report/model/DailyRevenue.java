package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.time.LocalDate;

/** O faturamento de um dia do restaurante e quantas comandas fecharam nele. */
public record DailyRevenue(LocalDate day, Money revenue, int tabs) {
}
