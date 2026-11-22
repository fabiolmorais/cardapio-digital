package br.com.cardapio.domain.report.model;

import java.time.Duration;

/** Um tempo médio numa hora do dia, e de quantos casos (a resposta ao chamado por hora, REL-38). */
public record HourDuration(int hour, Duration average, int count) {
}
