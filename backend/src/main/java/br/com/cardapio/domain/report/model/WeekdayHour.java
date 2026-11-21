package br.com.cardapio.domain.report.model;

/** Uma hora de um dia da semana (1 = segunda … 7 = domingo; 0 a 23h). */
public record WeekdayHour(int isoWeekday, int hour) {
}
