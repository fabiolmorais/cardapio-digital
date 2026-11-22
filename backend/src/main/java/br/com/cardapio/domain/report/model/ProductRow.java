package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

/**
 * Uma linha da tabela de produtos com o que o Business vê a mais (REL-15, 23).
 *
 * @param before    o que os produtos à frente dele (por faturamento) somam: a base da curva ABC
 * @param total     o faturamento de todos os produtos do recorte
 * @param compared  o faturamento do produto no período anterior
 * @param canceled  as unidades canceladas nas comandas fechadas do período
 */
public record ProductRow(ProductSales sales, Money before, Money total, Money compared, int canceled) {

    public AbcClass abc() {
        return AbcClass.of(before, total);
    }
}
