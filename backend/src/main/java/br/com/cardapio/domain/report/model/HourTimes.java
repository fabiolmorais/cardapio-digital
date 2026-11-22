package br.com.cardapio.domain.report.model;

import java.time.Duration;

/** Os itens entregues que chegaram à cozinha numa hora do dia: o preparo e o passe médios (REL-32). */
public record HourTimes(int hour, Duration prep, Duration passe, int items) {
}
