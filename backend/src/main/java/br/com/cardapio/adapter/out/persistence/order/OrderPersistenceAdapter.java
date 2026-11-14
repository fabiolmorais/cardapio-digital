package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.order.KitchenItemView;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.WaitingOrderView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
class OrderPersistenceAdapter implements OrderPort {

    private final OrderJpaRepository orders;
    private final OrderItemJpaRepository items;
    private final OrderItemChoices choices;
    private final EstablishmentFilter establishmentFilter;

    OrderPersistenceAdapter(OrderJpaRepository orders, OrderItemJpaRepository items, OrderItemChoices choices,
                            EstablishmentFilter establishmentFilter) {
        this.orders = orders;
        this.items = items;
        this.choices = choices;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public int nextNumber(EstablishmentId establishmentId, Instant now) {
        return orders.nextNumber(establishmentId.value(), now);
    }

    @Override
    public void insert(Order o, List<OrderItem> orderItems) {
        establishmentFilter.enable(o.establishmentId());
        orders.save(new OrderJpaEntity(o.id().value(), o.establishmentId().value(), o.tabId().value(),
                o.tableId().value(), o.sessionId().value(), o.number(), o.status().name(), o.createdAt(),
                o.approvedBy() == null ? null : o.approvedBy().value(), o.approvedAt()));
        // A posição de cada item é a ordem dele na lista (a do carrinho).
        items.saveAll(IntStream.range(0, orderItems.size()).mapToObj(position -> {
            OrderItem i = orderItems.get(position);
            return new OrderItemJpaEntity(i.id().value(), i.establishmentId().value(), i.orderId().value(), position,
                    i.productId().value(), i.productName(), i.unitPrice().cents(), i.quantity(), i.note(),
                    i.status().name(), i.createdAt(), i.tabRodizioId() == null ? null : i.tabRodizioId().value(),
                    i.choices().variantId() == null ? null : i.choices().variantId().value(),
                    i.choices().variantName());
        }).toList());
        choices.insert(orderItems);
    }

    @Override
    public long countPlacedBySessionAfter(EstablishmentId establishmentId, TableSessionId sessionId, Instant instant) {
        establishmentFilter.enable(establishmentId);
        return orders.countByEstablishmentIdAndSessionIdAndCreatedAtAfter(establishmentId.value(), sessionId.value(),
                instant);
    }

    /** Duas consultas: os pedidos da comanda, depois os itens de todos eles (IN). Junta em memória. */
    @Override
    public List<OrderWithItems> findByTab(EstablishmentId establishmentId, TabId tabId) {
        establishmentFilter.enable(establishmentId);
        List<OrderJpaEntity> found = orders.findByEstablishmentIdAndTabIdOrderByCreatedAtAscNumberAsc(
                establishmentId.value(), tabId.value());
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<OrderItem>> byOrder = choices.toDomain(establishmentId.value(),
                        items.findByEstablishmentIdAndOrderIdInOrderByPositionAsc(establishmentId.value(),
                                found.stream().map(OrderJpaEntity::getId).toList())).stream()
                .collect(Collectors.groupingBy(i -> i.orderId().value()));
        return found.stream()
                .map(o -> new OrderWithItems(toDomain(o), byOrder.getOrDefault(o.getId(), List.of())))
                .toList();
    }

    @Override
    public Optional<Order> findOrder(EstablishmentId establishmentId, OrderId id) {
        establishmentFilter.enable(establishmentId);
        return orders.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<OrderItem> findItem(EstablishmentId establishmentId, OrderItemId id) {
        establishmentFilter.enable(establishmentId);
        return items.findByIdAndEstablishmentId(id.value(), establishmentId.value())
                .map(e -> choices.toDomain(establishmentId.value(), List.of(e)).getFirst());
    }

    @Override
    public boolean updateItem(OrderItem i, OrderItemStatus expectedStatus) {
        establishmentFilter.enable(i.establishmentId());
        return items.updateIfStatus(i.id().value(), i.establishmentId().value(), expectedStatus.name(),
                i.status().name(), i.preparingAt(), i.readyAt(), i.deliveredAt(),
                i.deliveredBy() == null ? null : i.deliveredBy().value(), i.canceledAt(),
                i.canceledBy() == null ? null : i.canceledBy().value(), i.cancelReason()) == 1;
    }

    @Override
    public boolean splitItem(OrderItem kept, OrderItem canceled, OrderItemStatus expectedStatus) {
        establishmentFilter.enable(kept.establishmentId());
        OrderItemJpaEntity original = items.findByIdAndEstablishmentId(kept.id().value(),
                kept.establishmentId().value()).orElse(null);
        if (original == null) {
            return false;
        }
        int before = kept.quantity() + canceled.quantity();
        if (items.updateQuantityIfUnchanged(kept.id().value(), kept.establishmentId().value(), expectedStatus.name(),
                before, kept.quantity()) != 1) {
            return false;
        }
        items.save(original.canceledPart(canceled.id().value(), canceled.quantity(), canceled.canceledAt(),
                canceled.canceledBy() == null ? null : canceled.canceledBy().value(), canceled.cancelReason()));
        items.flush();
        choices.copyTo(canceled);
        return true;
    }

    @Override
    public boolean updateOrder(Order o, OrderStatus expectedStatus) {
        establishmentFilter.enable(o.establishmentId());
        return orders.updateIfStatus(o.id().value(), o.establishmentId().value(), expectedStatus.name(),
                o.status().name(), o.approvedBy() == null ? null : o.approvedBy().value(), o.approvedAt()) == 1;
    }

    @Override
    public void cancelItemsOnTheWay(EstablishmentId establishmentId, OrderId orderId, UserId by, String reason,
                                    Instant now) {
        establishmentFilter.enable(establishmentId);
        items.cancelOnTheWay(orderId.value(), establishmentId.value(), by.value(), reason, now);
    }

    @Override
    public ResultPage<KitchenItemView> findQueue(EstablishmentId establishmentId, Set<OrderItemStatus> statuses,
                                                 PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<KitchenItemRow> rows = items.findQueue(establishmentId.value(),
                statuses.stream().map(Enum::name).toList(), PageRequest.of(page.page(), page.size()));
        return kitchenPage(establishmentId, rows, page);
    }

    @Override
    public ResultPage<KitchenItemView> findPassagemQueue(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<KitchenItemRow> rows = items.findPassagemQueue(establishmentId.value(),
                PageRequest.of(page.page(), page.size()));
        return kitchenPage(establishmentId, rows, page);
    }

    /** A página da fila, com as escolhas de todos os itens numa consulta só. */
    private ResultPage<KitchenItemView> kitchenPage(EstablishmentId establishmentId, Page<KitchenItemRow> rows,
                                                    PageQuery page) {
        List<KitchenItemRow> content = rows.getContent();
        List<OrderItem> found = choices.toDomain(establishmentId.value(),
                content.stream().map(KitchenItemRow::item).toList());
        List<KitchenItemView> views = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            KitchenItemRow r = content.get(i);
            views.add(new KitchenItemView(found.get(i), r.orderNumber(), new DiningTableId(r.tableId()),
                    r.tableLabel(), r.sectorName()));
        }
        return new ResultPage<>(views, page.page(), page.size(), rows.getTotalElements());
    }

    /** Duas consultas: a página de pedidos (com a mesa), depois os itens de todos eles (IN). */
    @Override
    public ResultPage<WaitingOrderView> findWaitingApproval(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<WaitingOrderRow> rows = orders.findWaitingApproval(establishmentId.value(),
                PageRequest.of(page.page(), page.size()));
        Map<UUID, List<OrderItem>> byOrder = rows.isEmpty() ? Map.of()
                : choices.toDomain(establishmentId.value(),
                        items.findByEstablishmentIdAndOrderIdInOrderByPositionAsc(establishmentId.value(),
                                rows.stream().map(r -> r.order().getId()).toList())).stream()
                .collect(Collectors.groupingBy(i -> i.orderId().value()));
        return new ResultPage<>(rows.map(r -> new WaitingOrderView(toDomain(r.order()), r.tableLabel(),
                r.sectorName(), byOrder.getOrDefault(r.order().getId(), List.of()))).getContent(), page.page(),
                page.size(), rows.getTotalElements());
    }

    private static Order toDomain(OrderJpaEntity e) {
        return new Order(new OrderId(e.getId()), new EstablishmentId(e.getEstablishmentId()), new TabId(e.getTabId()),
                new DiningTableId(e.getTableId()), new TableSessionId(e.getSessionId()), e.getNumber(),
                OrderStatus.valueOf(e.getStatus()), e.getCreatedAt(),
                e.getApprovedBy() == null ? null : new UserId(e.getApprovedBy()), e.getApprovedAt());
    }
}
