package br.com.cardapio.application.service.live;

import br.com.cardapio.application.port.in.live.GetLiveFloorUseCase;
import br.com.cardapio.application.port.in.live.LiveFloor;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.call.CallView;
import br.com.cardapio.application.port.out.live.LiveFloorPort;
import br.com.cardapio.application.port.out.live.LiveTableView;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.WaitingOrderView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.DiningTableId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetLiveFloorService implements GetLiveFloorUseCase {

    /** Uma página basta: nenhum salão tem mais de 100 chamados ou pedidos parados ao mesmo tempo. */
    private static final PageQuery ALL = new PageQuery(0, PageQuery.MAX_SIZE);

    private final LiveFloorPort floor;
    private final CallPort calls;
    private final OrderPort orders;
    private final Clock clock;

    public GetLiveFloorService(LiveFloorPort floor, CallPort calls, OrderPort orders, Clock clock) {
        this.floor = floor;
        this.calls = calls;
        this.orders = orders;
        this.clock = clock;
    }

    /** Quatro consultas, qualquer que seja o número de mesas: as mesas, os chamados, os pedidos parados e o resumo. */
    @Override
    @Transactional(readOnly = true)
    public LiveFloor get(EstablishmentId establishmentId, boolean withRevenue) {
        Instant now = clock.instant();
        // A fila já vem do chamado mais antigo para o mais novo.
        Map<DiningTableId, List<CallView>> openCalls = calls.findQueue(establishmentId, ALL).content().stream()
                .collect(Collectors.groupingBy(v -> v.call().tableId()));
        Map<DiningTableId, Long> waiting = orders.findWaitingApproval(establishmentId, ALL).content().stream()
                .collect(Collectors.groupingBy((WaitingOrderView v) -> v.order().tableId(), Collectors.counting()));
        List<LiveFloor.Table> tables = floor.findTables(establishmentId, now).stream()
                .map(t -> table(t, openCalls.getOrDefault(t.id(), List.of()), waiting.getOrDefault(t.id(), 0L)))
                .toList();
        int occupied = (int) tables.stream().filter(t -> t.state() != LiveFloor.State.FREE).count();
        var wait = floor.averageWaitSeconds(establishmentId, now);
        return new LiveFloor(tables, occupied, withRevenue ? floor.revenueOn(establishmentId, now) : null,
                wait.isPresent() ? wait.getAsLong() : null);
    }

    private static LiveFloor.Table table(LiveTableView t, List<CallView> open, long waitingOrders) {
        CallView bill = open.stream().filter(v -> v.call().type() == CallType.BILL).findFirst().orElse(null);
        // O que ainda espera alguém aceitar vem antes do que já está em atendimento.
        CallView waiting = open.stream().filter(v -> v.call().status() == CallStatus.PENDING).findFirst().orElse(null);
        CallView shown = bill != null ? bill : waiting != null ? waiting : open.stream().findFirst().orElse(null);
        // Chamando só enquanto ninguém aceitou: com o garçom indo, a mesa volta a ocupada.
        LiveFloor.State state = bill != null ? LiveFloor.State.BILL
                : waiting != null ? LiveFloor.State.CALLING
                : shown != null ? LiveFloor.State.OCCUPIED
                : t.phones() > 0 || t.tabId() != null || waitingOrders > 0 ? LiveFloor.State.OCCUPIED
                : LiveFloor.State.FREE;
        LiveFloor.Call call = shown == null ? null
                : new LiveFloor.Call(shown.call().type(), shown.call().status(), shown.call().paymentMethod(),
                        shown.call().createdAt(), shown.acceptedByName());
        return new LiveFloor.Table(t.id(), t.label(), t.sectorId(), t.sectorName(), state, t.phones(), t.tabId(),
                t.tabOpenedAt(), t.tabTotal(), t.itemsOnTheWay(), t.rodizioPeople(), call, (int) waitingOrders);
    }
}
