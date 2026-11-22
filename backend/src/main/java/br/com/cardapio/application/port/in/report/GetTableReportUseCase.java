package br.com.cardapio.application.port.in.report;

public interface GetTableReportUseCase {

    TableReport get(ReportQuery query);
}
