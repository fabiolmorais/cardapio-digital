package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.CloseTabUseCase;
import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.call.CallView;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabPaymentPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.event.TabClosed;
import br.com.cardapio.domain.order.exception.TabNotFoundException;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPayment;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CloseTabService implements CloseTabUseCase {

    private final TabPort tabs;
    private final OrderPort orders;
    private final CallPort calls;
    private final TableSessionPort sessions;
    private final TabRodizioPort tabRodizios;
    private final TabPaymentPort payments;
    private final EventPublisherPort events;
    private final Clock clock;

    public CloseTabService(TabPort tabs, OrderPort orders, CallPort calls, TableSessionPort sessions,
                           TabRodizioPort tabRodizios, TabPaymentPort payments, EventPublisherPort events,
                           Clock clock) {
        this.payments = payments;
        this.tabs = tabs;
        this.orders = orders;
        this.calls = calls;
        this.sessions = sessions;
        this.tabRodizios = tabRodizios;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Tudo numa transação: a comanda fecha (cobrando só o que a mesa ainda não pagou), os celulares da mesa saem (a próxima visita começa do
     * zero, com uma comanda nova no primeiro pedido) e os chamados abertos terminam. Se uma parte
     * falhar, nada vale.
     */
    @Override
    @Transactional
    public StaffTab close(EstablishmentId establishmentId, TabId tabId, UserId by, PaymentMethod paymentMethod) {
        TabView view = tabs.findView(establishmentId, tabId).orElseThrow(TabNotFoundException::new);
        List<OrderWithItems> found = orders.findByTab(establishmentId, tabId);
        Instant now = clock.instant();
        // A conta é pessoas × preço do rodízio (se houve) mais os itens à parte.
        TabRodizio rodizio = tabRodizios.findByTab(establishmentId, tabId).orElse(null);
        // O que a mesa já pagou adiantado sai da conta: o fechamento cobra só o que falta.
        List<TabPayment> paid = payments.findPayments(establishmentId, tabId);
        Tab.Closing closing = view.tab().close(by, paymentMethod,
                found.stream().flatMap(o -> o.items().stream()).toList(),
                rodizio == null ? Money.ZERO : rodizio.total(), paid, now);
        Tab closed = closing.tab();
        // Dois garçons fechando juntos, ou um item que chegou entre a leitura e a gravação: o UPDATE
        // condicional (só se ainda estiver aberta) deixa valer um só.
        if (!tabs.close(closed)) {
            throw new TabStateException("Esta comanda acabou de mudar. Atualize a tela.");
        }
        List<TabPayment> all = paid;
        if (closing.finalPayment() != null) {
            payments.add(closing.finalPayment());
            all = new ArrayList<>(paid);
            all.add(closing.finalPayment());
        }
        sessions.endTable(establishmentId, closed.tableId(), now);
        for (CallView open : calls.findOpenByTable(establishmentId, closed.tableId())) {
            Call call = open.call();
            Call done = call.closeWithTab(now);
            if (calls.update(done, call.status())) {
                events.publish(CallChanged.of(done, now));
            }
        }
        events.publish(new TabClosed(establishmentId, closed.tableId(), closed.id(), now));
        return StaffTab.from(new TabView(closed, view.tableLabel(), view.sectorName()), found, null, rodizio, all);
    }
}
