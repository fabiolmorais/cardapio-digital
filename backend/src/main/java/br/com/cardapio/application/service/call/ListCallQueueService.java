package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.ListCallQueueUseCase;
import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import org.springframework.transaction.annotation.Transactional;

public class ListCallQueueService implements ListCallQueueUseCase {

    private final CallPort calls;

    public ListCallQueueService(CallPort calls) {
        this.calls = calls;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<StaffCall> list(EstablishmentId establishmentId, PageQuery page) {
        return calls.findQueue(establishmentId, page).map(StaffCall::from);
    }
}
