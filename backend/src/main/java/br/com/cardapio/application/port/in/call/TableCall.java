package br.com.cardapio.application.port.in.call;

import br.com.cardapio.application.port.out.call.CallView;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;

import java.time.Instant;

/**
 * O chamado como a MESA vê. De quem aceitou, só o primeiro nome ("Carlos a caminho"): o cliente
 * não precisa do nome completo de ninguém da equipe.
 *
 * @param releaseCode o código do pedido de liberação. Só chega ao celular que pediu: a lista da
 *                    mesa não mostra os pedidos de liberação dos outros celulares.
 */
public record TableCall(CallId id, CallType type, PaymentMethod paymentMethod, CallStatus status,
                        Instant createdAt, Instant acceptedAt, String waiterFirstName, String releaseCode) {

    public static TableCall from(CallView view) {
        var call = view.call();
        String name = view.acceptedByName();
        String first = name == null ? null : name.trim().split("\\s+")[0];
        return new TableCall(call.id(), call.type(), call.paymentMethod(), call.status(), call.createdAt(),
                call.acceptedAt(), first, call.releaseCode());
    }
}
