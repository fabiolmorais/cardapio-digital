package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.in.order.BillSummary;
import br.com.cardapio.application.port.in.order.ReceiveBillPaymentUseCase;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.TabPaymentPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.exception.TabStateException;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabBalance;
import br.com.cardapio.domain.order.model.TabPayment;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ReceiveBillPaymentService implements ReceiveBillPaymentUseCase {

    private final CallPort calls;
    private final TabPort tabs;
    private final OrderPort orders;
    private final TabRodizioPort tabRodizios;
    private final TabPaymentPort payments;
    private final EventPublisherPort events;
    private final Clock clock;

    public ReceiveBillPaymentService(CallPort calls, TabPort tabs, OrderPort orders, TabRodizioPort tabRodizios,
                                     TabPaymentPort payments, EventPublisherPort events, Clock clock) {
        this.calls = calls;
        this.tabs = tabs;
        this.orders = orders;
        this.tabRodizios = tabRodizios;
        this.payments = payments;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public BillSummary bill(EstablishmentId establishmentId, CallId callId) {
        Call call = billCall(establishmentId, callId);
        Optional<Tab> open = tabs.findOpen(establishmentId, call.tableId());
        if (open.isEmpty()) {
            return new BillSummary(null, Money.ZERO, Money.ZERO, Money.ZERO, call.paymentMethod(), 0);
        }
        List<OrderItem> items = itemsOf(establishmentId, open.get());
        TabBalance balance = balanceOf(establishmentId, open.get(), items);
        int onTheWay = items.stream().filter(i -> i.status().isOnTheWay()).mapToInt(OrderItem::quantity).sum();
        return new BillSummary(open.get().id(), balance.total(), balance.paid(), balance.due(), call.paymentMethod(),
                onTheWay);
    }

    /**
     * Numa transação: o pagamento é gravado e o pedido de conta termina atendido. A comanda NÃO
     * fecha: quem fecha é o garçom, quando tudo tiver chegado à mesa (e aí cobra só o que faltar).
     */
    @Override
    @Transactional
    public StaffCall receive(EstablishmentId establishmentId, CallId callId, UserId by, Money amount,
                             PaymentMethod method) {
        Call call = billCall(establishmentId, callId);
        Tab tab = tabs.findOpen(establishmentId, call.tableId())
                .orElseThrow(() -> new TabStateException("A mesa ainda não tem nada na conta"));
        Instant now = clock.instant();
        TabPayment payment = TabPayment.receive(tab, balanceOf(establishmentId, tab, itemsOf(establishmentId, tab)),
                amount, method, by, call.id(), now);
        Call done = call.finish(now);
        // Dois garçons recebendo juntos: o UPDATE condicional deixa valer um só (e o pagamento junto).
        if (!calls.update(done, CallStatus.IN_PROGRESS)) {
            throw new CallStateException("Este chamado acabou de mudar. Atualize a fila.");
        }
        payments.add(payment);
        events.publish(CallChanged.of(done, now));
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }

    private Call billCall(EstablishmentId establishmentId, CallId callId) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        if (call.type() != CallType.BILL) {
            throw new CallStateException("Só o pedido de conta tem valor a receber");
        }
        return call;
    }

    private List<OrderItem> itemsOf(EstablishmentId establishmentId, Tab tab) {
        return orders.findByTab(establishmentId, tab.id()).stream().flatMap(o -> o.items().stream()).toList();
    }

    private TabBalance balanceOf(EstablishmentId establishmentId, Tab tab, List<OrderItem> items) {
        Money rodizio = tabRodizios.findByTab(establishmentId, tab.id()).map(TabRodizio::total).orElse(Money.ZERO);
        return TabBalance.of(items, rodizio, payments.findPayments(establishmentId, tab.id()));
    }
}
