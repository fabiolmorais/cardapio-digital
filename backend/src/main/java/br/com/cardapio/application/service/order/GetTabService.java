package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.GetTabUseCase;
import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.TabMovePort;
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
import br.com.cardapio.application.port.out.order.OrderWithItems;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class GetTabService implements GetTabUseCase {

    private final TabPort tabs;
    private final OrderPort orders;
    private final CallPort calls;
    private final TabRodizioPort tabRodizios;
    private final TabPaymentPort payments;
    private final TabMovePort moves;

    public GetTabService(TabPort tabs, OrderPort orders, CallPort calls, TabRodizioPort tabRodizios,
                         TabPaymentPort payments, TabMovePort moves) {
        this.payments = payments;
        this.moves = moves;
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
        List<OrderWithItems> found = orders.findByTab(establishmentId, tabId);
        return StaffTab.from(view, found, billRequestedWith,
                tabRodizios.findByTab(establishmentId, tabId).orElse(null), payments.findPayments(establishmentId, tabId),
                orders.findStaffNames(establishmentId, StaffTab.staffOf(found)))
                .withMoves(moves.findByTab(establishmentId, tabId),
                        moves.findMergedInto(establishmentId, tabId).orElse(null));
    }
}
