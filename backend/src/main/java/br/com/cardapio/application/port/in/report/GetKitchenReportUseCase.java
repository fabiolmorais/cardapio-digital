package br.com.cardapio.application.port.in.report;

public interface GetKitchenReportUseCase {

    KitchenReport get(KitchenReportQuery query);
}
