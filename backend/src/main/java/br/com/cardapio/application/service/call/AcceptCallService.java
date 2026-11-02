package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.AcceptCallUseCase;
import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class AcceptCallService implements AcceptCallUseCase {

    private final CallPort calls;
    private final Clock clock;

    public AcceptCallService(CallPort calls, Clock clock) {
        this.calls = calls;
        this.clock = clock;
    }

    /**
     * Dois garçons tocam em "Aceitar" quase juntos: os dois leem o chamado pendente e os dois
     * passam pelo accept() do domínio. Quem decide é o UPDATE condicional ("só se ainda estiver
     * pendente"): o banco aplica um, e o outro recebe 409.
     */
    @Override
    @Transactional
    public StaffCall accept(EstablishmentId establishmentId, CallId callId, UserId by) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        if (!calls.update(call.accept(by, clock.instant()), CallStatus.PENDING)) {
            throw new CallStateException("Outra pessoa já aceitou este chamado");
        }
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }
}
