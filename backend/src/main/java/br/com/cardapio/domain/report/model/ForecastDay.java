package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/** Um dia da semana igual a hoje, nas semanas anteriores: quanto fechou e quanto tinha nesta mesma hora. */
public record ForecastDay(Money full, Money untilNow) {
}
