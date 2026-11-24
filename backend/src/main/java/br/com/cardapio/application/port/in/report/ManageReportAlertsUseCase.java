package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.AlertSettings;

/** A configuração dos alertas em tempo real na gestão (Business; só o dono e os gerentes). */
public interface ManageReportAlertsUseCase {

    AlertSettings get(EstablishmentId establishmentId);

    AlertSettings update(EstablishmentId establishmentId, AlertSettings settings);
}
