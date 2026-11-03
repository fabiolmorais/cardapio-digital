package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.application.port.out.order.KitchenItemView;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.WaitingOrderView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductDetails;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * O cardápio, as configurações, as comandas e os pedidos na memória, para os testes dos serviços
 * do pedido. Do cardápio, só o que o pedido usa; o resto lança UnsupportedOperationException.
 */
final class OrderFakes implements ProductPort, OrderSettingsPort, TabPort, OrderPort {

    final Map<ProductId, Product> products = new LinkedHashMap<>();
    final Map<EstablishmentId, OrderSettings> settings = new HashMap<>();
    final Map<TabId, Tab> tabs = new LinkedHashMap<>();
    final Map<OrderId, Order> orders = new LinkedHashMap<>();
    final Map<OrderItemId, OrderItem> items = new LinkedHashMap<>();
    private final Map<EstablishmentId, Integer> counters = new HashMap<>();

    Product product(EstablishmentId establishmentId, String name, long priceCents, boolean available) {
        Product product = Product.create(establishmentId, MenuCategoryId.newId(),
                new ProductDetails(name, "", Money.ofCents(priceCents), null, false), available, products.size());
        products.put(product.id(), product);
        return product;
    }

    List<OrderItem> itemsOf(OrderId orderId) {
        return items.values().stream().filter(i -> i.orderId().equals(orderId)).toList();
    }

    // --- ProductPort
    @Override
    public List<Product> findActiveByIds(EstablishmentId establishmentId, Collection<ProductId> ids) {
        return products.values().stream()
                .filter(p -> p.establishmentId().equals(establishmentId) && p.active() && ids.contains(p.id()))
                .toList();
    }

    @Override
    public void save(Product product) { throw new UnsupportedOperationException(); }

    @Override
    public Optional<Product> find(EstablishmentId establishmentId, ProductId id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultPage<Product> search(EstablishmentId establishmentId, MenuCategoryId categoryId, String text,
                                      PageQuery page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Product> findActiveByCategory(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId, MenuCategoryId categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countActive(EstablishmentId establishmentId) { throw new UnsupportedOperationException(); }

    @Override
    public List<Product> findAllActive(EstablishmentId establishmentId) { throw new UnsupportedOperationException(); }

    // --- OrderSettingsPort
    @Override
    public Optional<OrderSettings> find(EstablishmentId establishmentId) {
        return Optional.ofNullable(settings.get(establishmentId));
    }

    @Override
    public void save(OrderSettings s) {
        settings.put(s.establishmentId(), s);
    }

    // --- TabPort
    @Override
    public Optional<Tab> findOpen(EstablishmentId establishmentId, DiningTableId tableId) {
        return tabs.values().stream().filter(t -> t.establishmentId().equals(establishmentId)
                && t.tableId().equals(tableId) && t.status().isOpen()).findFirst();
    }

    @Override
    public boolean insertIfNoneOpen(Tab tab) {
        if (findOpen(tab.establishmentId(), tab.tableId()).isPresent()) {
            return false;
        }
        tabs.put(tab.id(), tab);
        return true;
    }

    // --- OrderPort
    @Override
    public int nextNumber(EstablishmentId establishmentId, Instant now) {
        return counters.merge(establishmentId, 1, Integer::sum);
    }

    @Override
    public void insert(Order order, List<OrderItem> orderItems) {
        orders.put(order.id(), order);
        orderItems.forEach(i -> items.put(i.id(), i));
    }

    @Override
    public long countPlacedBySessionAfter(EstablishmentId establishmentId, TableSessionId sessionId, Instant instant) {
        return orders.values().stream().filter(o -> o.establishmentId().equals(establishmentId)
                && o.sessionId().equals(sessionId) && o.createdAt().isAfter(instant)).count();
    }

    @Override
    public List<OrderWithItems> findByTab(EstablishmentId establishmentId, TabId tabId) {
        List<OrderWithItems> found = new ArrayList<>();
        for (Order o : orders.values()) {
            if (o.establishmentId().equals(establishmentId) && o.tabId().equals(tabId)) {
                found.add(new OrderWithItems(o, itemsOf(o.id())));
            }
        }
        return found;
    }

    @Override
    public Optional<Order> findOrder(EstablishmentId establishmentId, OrderId id) {
        return Optional.ofNullable(orders.get(id)).filter(o -> o.establishmentId().equals(establishmentId));
    }

    @Override
    public Optional<OrderItem> findItem(EstablishmentId establishmentId, OrderItemId id) {
        return Optional.ofNullable(items.get(id)).filter(i -> i.establishmentId().equals(establishmentId));
    }

    @Override
    public boolean updateItem(OrderItem changed, OrderItemStatus expectedStatus) {
        OrderItem current = items.get(changed.id());
        if (current == null || current.status() != expectedStatus) {
            return false;
        }
        items.put(changed.id(), changed);
        return true;
    }

    @Override
    public boolean updateOrder(Order changed, OrderStatus expectedStatus) {
        Order current = orders.get(changed.id());
        if (current == null || current.status() != expectedStatus) {
            return false;
        }
        orders.put(changed.id(), changed);
        return true;
    }

    @Override
    public void cancelItemsOnTheWay(EstablishmentId establishmentId, OrderId orderId, UserId by, String reason,
                                    Instant now) {
        for (OrderItem i : itemsOf(orderId)) {
            if (i.status().isOnTheWay()) {
                items.put(i.id(), i.cancelByStaff(by, reason, now));
            }
        }
    }

    /** A mesa e o setor não existem aqui: a fila falsa mostra "?" no lugar deles. */
    @Override
    public ResultPage<KitchenItemView> findQueue(EstablishmentId establishmentId, Set<OrderItemStatus> statuses,
                                                 PageQuery page) {
        List<KitchenItemView> found = items.values().stream()
                .filter(i -> i.establishmentId().equals(establishmentId) && statuses.contains(i.status()))
                .filter(i -> orders.get(i.orderId()).status() == OrderStatus.SENT)
                .sorted(Comparator.comparing(OrderItem::createdAt))
                .map(i -> {
                    Order o = orders.get(i.orderId());
                    return new KitchenItemView(i, o.number(), o.tableId(), "?", "?");
                })
                .toList();
        return new ResultPage<>(found, page.page(), page.size(), found.size());
    }

    @Override
    public ResultPage<WaitingOrderView> findWaitingApproval(EstablishmentId establishmentId, PageQuery page) {
        List<WaitingOrderView> found = orders.values().stream()
                .filter(o -> o.establishmentId().equals(establishmentId)
                        && o.status() == OrderStatus.PENDING_APPROVAL)
                .map(o -> new WaitingOrderView(o, "?", "?", itemsOf(o.id())))
                .toList();
        return new ResultPage<>(found, page.page(), page.size(), found.size());
    }
}
