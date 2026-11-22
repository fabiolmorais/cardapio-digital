package br.com.cardapio.application.port.in.report;

public interface GetTeamReportUseCase {

    TeamReport get(ReportQuery query);
}
