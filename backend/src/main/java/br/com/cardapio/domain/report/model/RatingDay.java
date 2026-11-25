package br.com.cardapio.domain.report.model;

import java.time.LocalDate;

/** As avaliações de um dia do restaurante: quantas e a soma das estrelas. */
public record RatingDay(LocalDate day, int count, long starSum) {
}
