package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.ServiceChoice;

import java.util.Objects;

/**
 * Um toque num botão da mesa.
 *
 * @param sessionToken  o cookie da sessão do celular; null se não veio
 * @param paymentMethod só no pedido de conta
 * @param service       no pedido de conta: a escolha da mesa para a taxa de serviço (RF39); null: não disse
 */
public record RequestCallCommand(String tableToken, String sessionToken, CallType type, PaymentMethod paymentMethod,
                                 ServiceChoice service) {

    public RequestCallCommand {
        Objects.requireNonNull(type);
    }

    public RequestCallCommand(String tableToken, String sessionToken, CallType type, PaymentMethod paymentMethod) {
        this(tableToken, sessionToken, type, paymentMethod, null);
    }
}
