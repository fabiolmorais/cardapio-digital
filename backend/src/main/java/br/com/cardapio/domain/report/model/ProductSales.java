package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.model.Money;

/** Um produto no período: quantas unidades vendeu e quanto somou (sem os cancelados), com a categoria. */
public record ProductSales(ProductId productId, String name, String category, int quantity, Money revenue) {
}
