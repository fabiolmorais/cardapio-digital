package br.com.cardapio.application.port.in.call;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

public interface ListCallQueueUseCase {

    ResultPage<StaffCall> list(EstablishmentId establishmentId, PageQuery page);
}
