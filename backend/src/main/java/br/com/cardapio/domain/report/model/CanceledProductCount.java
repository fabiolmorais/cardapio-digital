package br.com.cardapio.domain.report.model;

/** Quantas vezes um produto foi cancelado no período por um motivo (o texto do motivo; null sem motivo). */
public record CanceledProductCount(String name, String reason, int times) {
}
