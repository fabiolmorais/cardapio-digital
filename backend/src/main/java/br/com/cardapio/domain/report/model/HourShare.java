package br.com.cardapio.domain.report.model;

/** A ocupação média numa hora do dia: a fração das mesas com comanda aberta (0 a 1). */
public record HourShare(int hour, double share) {
}
