package br.com.cardapio.application.port.in.report;

public interface GetOverviewReportUseCase {

    OverviewReport get(ReportQuery query);
}
