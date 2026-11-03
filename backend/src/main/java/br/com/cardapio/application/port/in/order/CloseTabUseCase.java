package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.TabId;

public interface CloseTabUseCase {

    /**
     * Fecha a comanda depois do pagamento: grava o total e a forma de pagamento, encerra as
     * sessões dos celulares da mesa e conclui os chamados que ficaram abertos.
     */
    StaffTab close(EstablishmentId establishmentId, TabId tabId, UserId by, PaymentMethod paymentMethod);
}
