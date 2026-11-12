package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.GetTabUseCase;
import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.TabPaymentPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.model.TabId;
import org.springframework.transaction.annotation.Transactional;

public class GetTabService implements GetTabUseCase {

    private final TabPort tabs;
    private final OrderPort orders;
    private final CallPort calls;
    private final TabRodizioPort tabRodizios;
    private final TabPaymentPort payments;

    public GetTabService(TabPort tabs, OrderPort orders, CallPort calls, TabRodizioPort tabRodizios,
                         TabPaymentPort payments) {
        this.payments = payments;
        this.tabs = tabs;
        this.orders = orders;
        this.calls = calls;
        this.tabRodizios = tabRodizios;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffTab get(EstablishmentId establishmentId, TabId tabId) {
        TabView view = tabs.findView(establishmentId, tabId).orElseThrow(TabNotFoundException::new);
        // A conta pedida só importa enquanto a comanda está aberta.
        PaymentMethod billRequestedWith = view.tab().status().isOpen()
                ? calls.findOpen(establishmentId, view.tab().tableId(), CallType.BILL)
                        .map(Call::paymentMethod).orElse(null)
                : null;
        return StaffTab.from(view, orders.findByTab(establishmentId, tabId), billRequestedWith,
                tabRodizios.findByTab(establishmentId, tabId).orElse(null), payments.findPayments(establishmentId, tabId));
    }
}
