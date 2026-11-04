package br.com.cardapio.application.port.in.call;

import br.com.cardapio.application.port.out.call.CallView;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;

import java.time.Instant;

/** O chamado como a EQUIPE vê, na fila: de qual mesa, de qual setor, quem aceitou e como a mesa provou a presença. */
public record StaffCall(CallId id, DiningTableId tableId, String tableLabel, String sectorName, CallType type,
                        PaymentMethod paymentMethod, CallStatus status, Instant createdAt, Instant acceptedAt,
                        String acceptedByName, PresenceMethod presenceMethod) {

    public static StaffCall from(CallView view) {
        var call = view.call();
        return new StaffCall(call.id(), call.tableId(), view.tableLabel(), view.sectorName(), call.type(),
                call.paymentMethod(), call.status(), call.createdAt(), call.acceptedAt(), view.acceptedByName(),
                view.presenceMethod());
    }
}
