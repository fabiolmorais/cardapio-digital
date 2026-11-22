package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.AbcClass;
import br.com.cardapio.domain.report.model.ProductRow;
import br.com.cardapio.domain.report.model.ProductSales;
import br.com.cardapio.domain.shared.model.Money;

/**
 * Uma linha da tabela de produtos. No Business, com a curva ABC, o faturamento do período anterior e
 * os cancelados; no Pro, esses três ficam null (o dado não sai do servidor).
 */
public record MenuProduct(ProductSales sales, AbcClass abc, Money compared, Integer canceled) {

    public static MenuProduct basic(ProductRow row) {
        return new MenuProduct(row.sales(), null, null, null);
    }

    public static MenuProduct full(ProductRow row) {
        return new MenuProduct(row.sales(), row.abc(), row.compared(), row.canceled());
    }
}
