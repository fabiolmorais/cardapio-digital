package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.FinishCallUseCase;
import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class FinishCallService implements FinishCallUseCase {

    private final CallPort calls;
    private final Clock clock;

    public FinishCallService(CallPort calls, Clock clock) {
        this.calls = calls;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StaffCall finish(EstablishmentId establishmentId, CallId callId) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        if (!calls.update(call.finish(clock.instant()), CallStatus.IN_PROGRESS)) {
            throw new CallStateException("Este chamado acabou de mudar. Atualize a fila.");
        }
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }
}
