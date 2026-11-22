package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;

/** Quantas unidades de um tamanho do produto foram vendidas (REL-18). */
public record VariantSales(ProductId productId, String product, String variant, int quantity) {
}
