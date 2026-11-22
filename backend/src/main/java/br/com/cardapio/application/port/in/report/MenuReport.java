package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.CategorySales;
import br.com.cardapio.domain.report.model.ProductSales;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;

/**
 * O Cardápio (desenho 14.3), com os blocos básicos (REL-13, 14, 16). A tabela de produtos vem à parte,
 * paginada.
 *
 * @param itemsRevenue   o faturamento dos itens do período (a base da participação de cada produto)
 * @param itemsSold      as unidades vendidas no período
 * @param activeProducts os produtos ativos do cardápio de sempre
 * @param full           os blocos completos; null para quem só tem os básicos (o dado não sai do servidor)
 */
public record MenuReport(ReportLevel level, ReportPeriod period, Money itemsRevenue, int itemsSold,
                         int activeProducts, List<CategorySales> byCategory, List<ProductSales> slowSellers,
                         MenuFull full) {
}
