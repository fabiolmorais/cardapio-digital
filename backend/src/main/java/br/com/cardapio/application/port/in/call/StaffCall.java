package br.com.cardapio.application.port.in.call;

import br.com.cardapio.application.port.out.call.CallView;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;

import java.time.Instant;

/** O chamado como a EQUIPE vê, na fila: de qual mesa, de qual setor, e quem aceitou. */
public record StaffCall(CallId id, String tableLabel, String sectorName, CallType type, PaymentMethod paymentMethod,
                        CallStatus status, Instant createdAt, Instant acceptedAt, String acceptedByName) {

    public static StaffCall from(CallView view) {
        var call = view.call();
        return new StaffCall(call.id(), view.tableLabel(), view.sectorName(), call.type(), call.paymentMethod(),
                call.status(), call.createdAt(), call.acceptedAt(), view.acceptedByName());
    }
}
