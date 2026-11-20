package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPayment;

import java.util.List;

/** Os pagamentos recebidos pelas comandas. */
public interface TabPaymentPort {

    void add(TabPayment payment);

    /** Os pagamentos da comanda, do mais antigo ao mais novo. */
    List<TabPayment> findPayments(EstablishmentId establishmentId, TabId tabId);

    /** Na junção, os pagamentos adiantados da comanda de origem passam para a de destino. */
    void moveToTab(EstablishmentId establishmentId, TabId fromTab, TabId toTab);
}
