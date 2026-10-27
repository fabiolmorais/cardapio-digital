package br.com.cardapio.application.port.in.device;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

public interface ListDevicesUseCase {

    ResultPage<DeviceSummary> list(EstablishmentId establishmentId, PageQuery page);
}
