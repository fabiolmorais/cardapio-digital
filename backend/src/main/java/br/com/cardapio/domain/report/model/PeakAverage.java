package br.com.cardapio.domain.report.model;

import java.math.BigDecimal;

/** A média do pico de uma hora do dia, nos dias em que a cozinha trabalhou nessa hora (REL-34). */
public record PeakAverage(int hour, BigDecimal average) {
}
