package br.com.cardapio.application.port.in.order;

import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.shared.model.Money;

/**
 * O garçom atende o pedido de conta recebendo o pagamento: a comanda continua aberta (ainda pode
 * haver itens a caminho) e o que a mesa pagou sai do que falta.
 */
public interface ReceiveBillPaymentUseCase {

    /** O que a mesa deve, para o garçom conferir antes de receber. */
    BillSummary bill(EstablishmentId establishmentId, CallId callId);

    /** Recebe o valor e conclui o pedido de conta. */
    StaffCall receive(EstablishmentId establishmentId, CallId callId, UserId by, Money amount, PaymentMethod method);
}
