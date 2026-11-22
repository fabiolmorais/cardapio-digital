package br.com.cardapio.domain.report.model;

import java.time.LocalDate;

/** O máximo de itens em preparo ao mesmo tempo numa hora de um dia (REL-34). */
public record HourPeak(LocalDate day, int hour, int peak) {
}
