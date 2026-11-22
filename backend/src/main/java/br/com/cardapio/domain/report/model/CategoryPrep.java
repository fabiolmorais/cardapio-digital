package br.com.cardapio.domain.report.model;

import java.time.Duration;

/** O preparo médio de uma categoria (REL-36). */
public record CategoryPrep(String category, int preparations, Duration average) {
}
