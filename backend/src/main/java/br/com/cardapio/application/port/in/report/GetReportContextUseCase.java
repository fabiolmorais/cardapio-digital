package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;

public interface GetReportContextUseCase {

    /** O contexto do restaurante de quem pede; a pessoa decide se a comparação entre unidades aparece. */
    ReportContext get(EstablishmentId establishmentId, UserId userId);
}
