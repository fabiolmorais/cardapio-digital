package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;

import java.util.Objects;

/**
 * Um toque num botão da mesa.
 *
 * @param sessionToken  o cookie da sessão do celular; null se não veio
 * @param paymentMethod só no pedido de conta
 */
/**
 * @param serviceAccepted no pedido de conta: a mesa aceitou a taxa de serviço (RF39)? null: não disse
 */
public record RequestCallCommand(String tableToken, String sessionToken, CallType type, PaymentMethod paymentMethod,
                                 Boolean serviceAccepted) {

    public RequestCallCommand {
        Objects.requireNonNull(type);
    }

    public RequestCallCommand(String tableToken, String sessionToken, CallType type, PaymentMethod paymentMethod) {
        this(tableToken, sessionToken, type, paymentMethod, null);
    }
}
