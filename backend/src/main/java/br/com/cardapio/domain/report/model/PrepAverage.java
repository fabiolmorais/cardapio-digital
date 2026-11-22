package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Duration;

/** O preparo médio de um produto num intervalo, e de quantos preparos (somado no banco). */
public record PrepAverage(ProductId productId, String name, int preparations, Duration average) {
}
