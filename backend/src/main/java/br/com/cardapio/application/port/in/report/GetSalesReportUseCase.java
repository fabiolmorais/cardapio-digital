package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.SeriesGrouping;

public interface GetSalesReportUseCase {

    SalesReport get(ReportQuery query, SeriesGrouping grouping);
}
