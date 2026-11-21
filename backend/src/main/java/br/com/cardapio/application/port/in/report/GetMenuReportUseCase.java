package br.com.cardapio.application.port.in.report;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.report.model.ProductSales;
import br.com.cardapio.domain.report.model.ProductSort;

public interface GetMenuReportUseCase {

    MenuReport get(MenuReportQuery query);

    /** A tabela de produtos do período, paginada (as listas da API são sempre paginadas). */
    ResultPage<ProductSales> products(MenuReportQuery query, ProductSort sort, PageQuery page);
}
