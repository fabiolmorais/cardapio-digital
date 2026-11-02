package br.com.cardapio.application.service.call;

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
import br.com.cardapio.domain.floor.model.DiningTableId;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** O CallPort na memória, com as duas garantias do banco: um aberto por tipo e o UPDATE condicional. */
class InMemoryCalls implements CallPort {

    final Map<CallId, Call> saved = new LinkedHashMap<>();
    final Map<UserId, String> names = new HashMap<>();

    @Override
    public boolean insertIfNoneOpen(Call call) {
        boolean taken = saved.values().stream().anyMatch(c -> c.isOpen() && c.tableId().equals(call.tableId())
                && c.type() == call.type());
        if (!taken) {
            saved.put(call.id(), call);
        }
        return !taken;
    }

    @Override
    public boolean update(Call changed, CallStatus expectedStatus) {
        Call current = saved.get(changed.id());
        if (current == null || current.status() != expectedStatus) {
            return false;
        }
        saved.put(changed.id(), changed);
        return true;
    }

    @Override
    public Optional<Call> find(EstablishmentId establishmentId, CallId id) {
        return Optional.ofNullable(saved.get(id)).filter(c -> c.establishmentId().equals(establishmentId));
    }

    @Override
    public Optional<CallView> findView(EstablishmentId establishmentId, CallId id) {
        return find(establishmentId, id).map(this::view);
    }

    @Override
    public Optional<Call> findOpen(EstablishmentId establishmentId, DiningTableId tableId, CallType type) {
        return saved.values().stream().filter(c -> c.establishmentId().equals(establishmentId)
                && c.tableId().equals(tableId) && c.type() == type && c.isOpen()).findFirst();
    }

    @Override
    public List<CallView> findOpenByTable(EstablishmentId establishmentId, DiningTableId tableId) {
        return saved.values().stream().filter(c -> c.establishmentId().equals(establishmentId)
                && c.tableId().equals(tableId) && c.isOpen()).map(this::view).toList();
    }

    @Override
    public ResultPage<CallView> findQueue(EstablishmentId establishmentId, PageQuery page) {
        List<CallView> open = saved.values().stream()
                .filter(c -> c.establishmentId().equals(establishmentId) && c.isOpen())
                .sorted(Comparator.comparing(Call::createdAt)).map(this::view).toList();
        return new ResultPage<>(open, page.page(), page.size(), open.size());
    }

    private CallView view(Call call) {
        return new CallView(call, "04", "Salão", call.acceptedBy() == null ? null : names.get(call.acceptedBy()));
    }
}
