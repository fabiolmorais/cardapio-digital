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

@Component
class KitchenPrintPersistenceAdapter implements KitchenPrintPort {

    private final KitchenPrintJpaRepository items;
    private final EstablishmentFilter establishmentFilter;

    KitchenPrintPersistenceAdapter(KitchenPrintJpaRepository items, EstablishmentFilter establishmentFilter) {
        this.items = items;
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
        return items.findHeader(id, orderId.value()).map(header -> view(header,
                (kind == TicketKind.ORDER
                        ? items.lockPendingOrderItems(id, orderId.value())
                        : items.lockPendingCancelItems(id, orderId.value()))
                        .stream().map(OrderPersistenceAdapter::toDomain).toList()));
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
                : lines.apply(establishmentId, headers.stream().map(PrintJobRow::orderId).toList()).stream()
                .map(OrderPersistenceAdapter::toDomain)
                .collect(Collectors.groupingBy(i -> i.orderId().value()));
        return new ResultPage<>(headers.map(h -> view(h, byOrder.getOrDefault(h.orderId(), List.of()))).getContent(),
                page.page(), page.size(), headers.getTotalElements());
    }

    private static PrintJobView view(PrintJobRow h, List<OrderItem> lines) {
        return new PrintJobView(new OrderId(h.orderId()), h.orderNumber(), new DiningTableId(h.tableId()),
                h.tableLabel(), h.sectorName(), lines);
    }
}
