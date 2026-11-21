package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface GetReportContextUseCase {

    ReportContext get(EstablishmentId establishmentId);
}
