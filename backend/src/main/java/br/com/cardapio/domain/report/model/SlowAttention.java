package br.com.cardapio.domain.report.model;

import java.time.Duration;

/** Um item que demorou bem mais que o normal no dia: o "ponto de atenção" do resumo. */
public record SlowAttention(String name, Duration dayAverage, Duration usualAverage) {
}
