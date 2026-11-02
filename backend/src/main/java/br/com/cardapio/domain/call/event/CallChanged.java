package br.com.cardapio.domain.call.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.event.TableEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Um chamado foi aberto, aceito, concluído, cancelado ou trocou a forma de pagamento.
 * Leva só o mínimo (os ids, o tipo e o status): quem precisar do resto busca pela API.
 */
public record CallChanged(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        CallId callId,
        CallType type,
        CallStatus status,
        Instant occurredAt
) implements TableEvent {

    public CallChanged {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(callId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
        Objects.requireNonNull(occurredAt);
    }

    public static CallChanged of(Call call, Instant now) {
        return new CallChanged(call.establishmentId(), call.tableId(), call.id(), call.type(), call.status(), now);
    }
}
