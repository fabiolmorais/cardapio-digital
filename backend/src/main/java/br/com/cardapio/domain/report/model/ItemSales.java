package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.model.Money;

/** Um produto vendido no período: quantas unidades e quanto somaram (sem os cancelados). */
public record ItemSales(ProductId productId, String name, int quantity, Money revenue) {
}
