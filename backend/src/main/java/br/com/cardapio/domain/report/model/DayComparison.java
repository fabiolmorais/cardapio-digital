package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.time.LocalDate;

/** Um dia do período ao lado do dia correspondente da comparação (o 1º com o 1º, e assim por diante). */
public record DayComparison(LocalDate day, Money revenue, LocalDate comparedDay, Money comparedRevenue) {
}
