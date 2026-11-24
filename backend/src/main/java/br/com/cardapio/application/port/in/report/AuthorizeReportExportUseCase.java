package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;

/** Confere se o plano do restaurante exporta os relatórios (RF29); sem a exportação, recusa. */
public interface AuthorizeReportExportUseCase {

    void authorize(EstablishmentId establishmentId);
}
