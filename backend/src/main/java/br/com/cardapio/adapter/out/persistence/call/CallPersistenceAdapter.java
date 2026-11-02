package br.com.cardapio.adapter.out.persistence.call;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.call.CallView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class CallPersistenceAdapter implements CallPort {

    private final CallJpaRepository calls;
    private final EstablishmentFilter establishmentFilter;

    CallPersistenceAdapter(CallJpaRepository calls, EstablishmentFilter establishmentFilter) {
        this.calls = calls;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public boolean insertIfNoneOpen(Call c) {
        return calls.insertIfNoneOpen(c.id().value(), c.establishmentId().value(), c.tableId().value(),
                c.sessionId().value(), c.type().name(), name(c.paymentMethod()), c.createdAt()) == 1;
    }

    @Override
    public boolean update(Call c, CallStatus expectedStatus) {
        establishmentFilter.enable(c.establishmentId());
        return calls.updateIfStatus(c.id().value(), c.establishmentId().value(), expectedStatus.name(),
                name(c.paymentMethod()), c.status().name(), c.acceptedAt(),
                c.acceptedBy() == null ? null : c.acceptedBy().value(), c.doneAt(), c.canceledAt()) == 1;
    }

    @Override
    public Optional<Call> find(EstablishmentId establishmentId, CallId id) {
        establishmentFilter.enable(establishmentId);
        return calls.findByIdAndEstablishmentId(id.value(), establishmentId.value()).map(CallPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<CallView> findView(EstablishmentId establishmentId, CallId id) {
        establishmentFilter.enable(establishmentId);
        return calls.findView(id.value(), establishmentId.value()).map(CallPersistenceAdapter::toView);
    }

    @Override
    public Optional<Call> findOpen(EstablishmentId establishmentId, DiningTableId tableId, CallType type) {
        establishmentFilter.enable(establishmentId);
        return calls.findOpen(establishmentId.value(), tableId.value(), type.name())
                .map(CallPersistenceAdapter::toDomain);
    }

    @Override
    public List<CallView> findOpenByTable(EstablishmentId establishmentId, DiningTableId tableId) {
        establishmentFilter.enable(establishmentId);
        return calls.findOpenByTable(establishmentId.value(), tableId.value()).stream()
                .map(CallPersistenceAdapter::toView).toList();
    }

    @Override
    public ResultPage<CallView> findQueue(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<CallRow> rows = calls.findQueue(establishmentId.value(), PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(CallPersistenceAdapter::toView).getContent(), page.page(), page.size(),
                rows.getTotalElements());
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static Call toDomain(CallJpaEntity e) {
        return new Call(new CallId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new DiningTableId(e.getTableId()), new TableSessionId(e.getSessionId()), CallType.valueOf(e.getType()),
                e.getPaymentMethod() == null ? null : PaymentMethod.valueOf(e.getPaymentMethod()),
                CallStatus.valueOf(e.getStatus()), e.getCreatedAt(), e.getAcceptedAt(),
                e.getAcceptedBy() == null ? null : new UserId(e.getAcceptedBy()), e.getDoneAt(), e.getCanceledAt());
    }

    private static CallView toView(CallRow r) {
        Call call = new Call(new CallId(r.getId()), new EstablishmentId(r.getEstablishmentId()),
                new DiningTableId(r.getTableId()), new TableSessionId(r.getSessionId()), CallType.valueOf(r.getType()),
                r.getPaymentMethod() == null ? null : PaymentMethod.valueOf(r.getPaymentMethod()),
                CallStatus.valueOf(r.getStatus()), r.getCreatedAt(), r.getAcceptedAt(),
                r.getAcceptedBy() == null ? null : new UserId(r.getAcceptedBy()), r.getDoneAt(), r.getCanceledAt());
        return new CallView(call, r.getTableLabel(), r.getSectorName(), r.getAcceptedByName());
    }
}
