package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.time.LocalDate;

/** Um ponto do faturamento no tempo: um dia, uma semana ou um mês (cortado nas pontas do período). */
public record SeriesPoint(LocalDate start, LocalDate end, Money revenue, int tabs) {
}
