package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.order.KitchenPrintPort;
import br.com.cardapio.application.port.out.order.PrintJobView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.TicketKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import br.com.cardapio.application.port.out.order.MovePrintView;
import br.com.cardapio.domain.order.model.TabMoveId;
import java.util.HashMap;

@Component
class KitchenPrintPersistenceAdapter implements KitchenPrintPort {

    private final KitchenPrintJpaRepository items;
    private final TabMoveJpaRepository moves;
    private final OrderItemChoices choices;
    private final EstablishmentFilter establishmentFilter;

    KitchenPrintPersistenceAdapter(KitchenPrintJpaRepository items, TabMoveJpaRepository moves,
                                   OrderItemChoices choices, EstablishmentFilter establishmentFilter) {
        this.items = items;
        this.moves = moves;
        this.choices = choices;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public ResultPage<PrintJobView> findPending(EstablishmentId establishmentId, TicketKind kind, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        UUID id = establishmentId.value();
        PageRequest pageable = PageRequest.of(page.page(), page.size());
        return kind == TicketKind.ORDER
                ? withItems(items.findPendingOrders(id, pageable), id, items::findPendingOrderItems, page)
                : withItems(items.findPendingCancellations(id, pageable), id, items::findPendingCancelItems, page);
    }

    @Override
    public ResultPage<PrintJobView> findPrintedSince(EstablishmentId establishmentId, Instant since, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        UUID id = establishmentId.value();
        return withItems(items.findPrintedSince(id, since, PageRequest.of(page.page(), page.size())), id,
                items::findPrintedItems, page);
    }

    @Override
    public Optional<PrintJobView> lockPending(EstablishmentId establishmentId, OrderId orderId, TicketKind kind) {
        establishmentFilter.enable(establishmentId);
        UUID id = establishmentId.value();
        return items.findHeader(id, orderId.value()).map(header -> view(header, choices.toDomain(id,
                kind == TicketKind.ORDER
                        ? items.lockPendingOrderItems(id, orderId.value())
                        : items.lockPendingCancelItems(id, orderId.value()))));
    }

    @Override
    public void markPrinted(EstablishmentId establishmentId, Collection<OrderItemId> itemIds, TicketKind kind,
                            Instant now) {
        if (itemIds.isEmpty()) {
            return;
        }
        establishmentFilter.enable(establishmentId);
        List<UUID> ids = itemIds.stream().map(OrderItemId::value).toList();
        if (kind == TicketKind.ORDER) {
            items.markOrderPrinted(establishmentId.value(), ids, now);
        } else {
            items.markCancelPrinted(establishmentId.value(), ids, now);
        }
    }

    /** A página de cabeçalhos e, numa consulta só (IN), os itens de todos eles. */
    private ResultPage<PrintJobView> withItems(Page<PrintJobRow> headers, UUID establishmentId,
                                               BiFunction<UUID, Collection<UUID>, List<OrderItemJpaEntity>> lines,
                                               PageQuery page) {
        Map<UUID, List<OrderItem>> byOrder = headers.isEmpty() ? Map.of()
                : choices.toDomain(establishmentId,
                        lines.apply(establishmentId, headers.stream().map(PrintJobRow::orderId).toList())).stream()
                .collect(Collectors.groupingBy(i -> i.orderId().value()));
        return new ResultPage<>(headers.map(h -> view(h, byOrder.getOrDefault(h.orderId(), List.of()))).getContent(),
                page.page(), page.size(), headers.getTotalElements());
    }

    private static PrintJobView view(PrintJobRow h, List<OrderItem> lines) {
        return new PrintJobView(new OrderId(h.orderId()), h.orderNumber(), new DiningTableId(h.tableId()),
                h.tableLabel(), h.sectorName(), lines);
    }

    @Override
    public ResultPage<MovePrintView> findPendingMoves(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<MovePrintRow> headers = moves.findPendingPrints(establishmentId.value(),
                PageRequest.of(page.page(), page.size()));
        Map<UUID, List<OrderItem>> byMove = moveItems(establishmentId.value(),
                headers.stream().map(MovePrintRow::moveId).toList());
        return new ResultPage<>(headers.map(h -> moveView(h, byMove.getOrDefault(h.moveId(), List.of())))
                .getContent(), page.page(), page.size(), headers.getTotalElements());
    }

    @Override
    public Optional<MovePrintView> lockPendingMove(EstablishmentId establishmentId, TabMoveId moveId) {
        establishmentFilter.enable(establishmentId);
        UUID id = establishmentId.value();
        if (moves.lock(id, moveId.value()).isEmpty()) {
            return Optional.empty();
        }
        return moves.findPrintHeader(id, moveId.value()).map(h -> {
            boolean pending = h.needsPrint() && h.printedAt() == null;
            return moveView(h, pending ? moveItems(id, List.of(h.moveId())).getOrDefault(h.moveId(), List.of())
                    : List.of());
        });
    }

    @Override
    public void markMovePrinted(EstablishmentId establishmentId, TabMoveId moveId, Instant now) {
        moves.markPrinted(establishmentId.value(), moveId.value(), now);
    }

    /** Os itens a caminho de cada via, numa consulta para os ids e outra (IN) para os itens. */
    private Map<UUID, List<OrderItem>> moveItems(UUID establishmentId, List<UUID> moveIds) {
        if (moveIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> moveOf = new HashMap<>();
        moves.findPrintItems(establishmentId, moveIds).forEach(r -> moveOf.put(r.getItemId(), r.getMoveId()));
        if (moveOf.isEmpty()) {
            return Map.of();
        }
        return choices.toDomain(establishmentId, items.findItems(establishmentId, moveOf.keySet())).stream()
                .collect(Collectors.groupingBy(i -> moveOf.get(i.id().value())));
    }

    private static MovePrintView moveView(MovePrintRow h, List<OrderItem> lines) {
        return new MovePrintView(new TabMoveId(h.moveId()), h.mergedTabId() != null, h.fromTableLabel(),
                h.toTableLabel(), h.sectorName(), h.movedAt(), lines);
    }
}
