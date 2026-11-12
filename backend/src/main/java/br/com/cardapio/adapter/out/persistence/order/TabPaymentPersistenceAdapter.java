package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.order.TabPaymentPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPayment;
import br.com.cardapio.domain.order.model.TabPaymentId;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class TabPaymentPersistenceAdapter implements TabPaymentPort {

    private final TabPaymentJpaRepository payments;
    private final EstablishmentFilter establishmentFilter;

    TabPaymentPersistenceAdapter(TabPaymentJpaRepository payments, EstablishmentFilter establishmentFilter) {
        this.payments = payments;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void add(TabPayment p) {
        establishmentFilter.enable(p.establishmentId());
        payments.save(new TabPaymentJpaEntity(p.id().value(), p.establishmentId().value(), p.tabId().value(),
                p.amount().cents(), p.method().name(), p.receivedAt(), p.receivedBy().value(),
                p.callId() == null ? null : p.callId().value()));
    }

    @Override
    public List<TabPayment> findPayments(EstablishmentId establishmentId, TabId tabId) {
        establishmentFilter.enable(establishmentId);
        return payments.findByEstablishmentIdAndTabIdOrderByReceivedAtAsc(establishmentId.value(), tabId.value())
                .stream()
                .map(e -> new TabPayment(new TabPaymentId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                        new TabId(e.getTabId()), Money.ofCents(e.getAmountCents()),
                        PaymentMethod.valueOf(e.getPaymentMethod()), e.getReceivedAt(),
                        new UserId(e.getReceivedBy()), e.getCallId() == null ? null : new CallId(e.getCallId())))
                .toList();
    }
}
