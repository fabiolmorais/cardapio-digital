package br.com.cardapio.domain.report.model;

import java.time.LocalDate;

/**
 * Um ponto da evolução da nota: um dia, ou uma semana com períodos longos.
 *
 * @param average null num intervalo sem avaliações
 */
public record RatingPoint(LocalDate from, LocalDate to, int count, Double average) {
}
