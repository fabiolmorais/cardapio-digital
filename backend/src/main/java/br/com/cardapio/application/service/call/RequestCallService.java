package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.RequestCallCommand;
import br.com.cardapio.application.port.in.call.RequestCallUseCase;
import br.com.cardapio.application.port.in.call.RequestedCall;
import br.com.cardapio.application.port.in.call.TableCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.security.RandomCodeGenerator;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.exception.TooManyCallsException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class RequestCallService implements RequestCallUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final CallPort calls;
    private final SubscriptionPort subscriptions;
    private final EventPublisherPort events;
    private final RandomCodeGenerator codes;
    private final Clock clock;

    public RequestCallService(DiningTablePort tables, TableSessionPort sessions, CallPort calls,
                              SubscriptionPort subscriptions, EventPublisherPort events, RandomCodeGenerator codes,
                              Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.calls = calls;
        this.subscriptions = subscriptions;
        this.events = events;
        this.codes = codes;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RequestedCall request(RequestCallCommand command) {
        Instant now = clock.instant();
        TableVisits.Visit visit = TableVisits.require(tables, sessions, command.tableToken(), command.sessionToken(),
                now);
        var establishmentId = visit.table().establishmentId();
        // Assinatura passou da tolerância: a mesa não chama nem pede pelo celular (Guia 31).
        PlanAllowances.requireServing(subscriptions, establishmentId, now);

        // Celular bloqueado pela equipe: nada, nem o pedido de liberação.
        if (visit.session().isBlocked()) {
            throw new SessionBlockedException();
        }
        // A trava de presença: chamar e pedir a conta, só no salão. O pedido de liberação é a saída
        // de quem não conseguiu provar (e não faz sentido para quem já provou).
        if (command.type() == CallType.UNLOCK) {
            if (visit.session().isPresent()) {
                throw new CallStateException("Sua mesa já está liberada.");
            }
        } else if (!visit.session().isPresent()) {
            throw new PresenceRequiredException();
        }

        // Já há um aberto deste tipo? É ele que volta (no máximo, com a forma de pagamento nova).
        // A liberação é de cada celular: só conta o aberto por ele mesmo.
        Optional<Call> open = findOpen(establishmentId, visit, command.type());
        if (open.isPresent()) {
            return new RequestedCall(view(changePaymentIfNeeded(open.get(), command)), false);
        }

        // Um chamado NOVO conta para o limite dos últimos 60 segundos (devolver o que já estava aberto, não).
        if (calls.countOpenedBySessionAfter(establishmentId, visit.session().id(), now.minus(Duration.ofMinutes(1)))
                >= Call.MAX_NEW_PER_MINUTE) {
            throw new TooManyCallsException();
        }
        Call call = command.type() == CallType.UNLOCK
                ? Call.openUnlock(establishmentId, visit.table().id(), visit.session().id(),
                        codes.digits(Call.RELEASE_CODE_LENGTH), now)
                : Call.open(establishmentId, visit.table().id(), visit.session().id(), command.type(),
                        command.paymentMethod(), now);
        if (calls.insertIfNoneOpen(call)) {
            events.publish(CallChanged.of(call, now));
            return new RequestedCall(view(call), true);
        }
        // Dois toques ao mesmo tempo: o outro gravou primeiro. Devolve o dele.
        Call theirs = findOpen(establishmentId, visit, command.type()).orElseThrow();
        return new RequestedCall(view(theirs), false);
    }

    private Optional<Call> findOpen(EstablishmentId establishmentId, TableVisits.Visit visit, CallType type) {
        if (type == CallType.UNLOCK) {
            return calls.findOpenBySession(establishmentId, visit.session().id()).stream()
                    .filter(call -> call.type() == CallType.UNLOCK)
                    .findFirst();
        }
        return calls.findOpen(establishmentId, visit.table().id(), type);
    }

    private Call changePaymentIfNeeded(Call open, RequestCallCommand command) {
        if (open.type() != CallType.BILL || command.paymentMethod() == null
                || command.paymentMethod() == open.paymentMethod()) {
            return open;
        }
        Call changed = open.withPaymentMethod(command.paymentMethod());
        if (calls.update(changed, open.status())) {
            events.publish(CallChanged.of(changed, clock.instant()));
            return changed;
        }
        // Se o garçom mexeu no chamado no meio do caminho, fica o que está gravado.
        return calls.find(open.establishmentId(), open.id()).orElse(open);
    }

    private TableCall view(Call call) {
        return TableCall.from(calls.findView(call.establishmentId(), call.id()).orElseThrow());
    }
}
