package br.com.cardapio.domain.report.model;

/**
 * A ordem da tabela de produtos: por faturamento ou por quantidade vendida (REL-13) e, no Business,
 * pelo crescimento do faturamento contra o período anterior.
 */
public enum ProductSort {
    REVENUE,
    QUANTITY,
    GROWTH
}
