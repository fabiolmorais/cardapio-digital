package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderPort {

    /**
     * O próximo número de pedido do restaurante NO DIA (nº 1, nº 2…), no fuso dele: recomeça à
     * meia-noite de lá. Dois pedidos ao mesmo tempo nunca recebem o mesmo número.
     */
    int nextNumber(EstablishmentId establishmentId, Instant now);

    /** Grava o pedido e os itens dele, juntos. */
    void insert(Order order, List<OrderItem> items);

    /** Quantos pedidos este celular mandou DEPOIS daquele instante (o limite por minuto). */
    long countPlacedBySessionAfter(EstablishmentId establishmentId, TableSessionId sessionId, Instant instant);

    /** Os pedidos da comanda com os itens, do mais antigo ao mais novo: duas consultas, sem N+1. */
    List<OrderWithItems> findByTab(EstablishmentId establishmentId, TabId tabId);

    Optional<Order> findOrder(EstablishmentId establishmentId, OrderId id);

    Optional<OrderItem> findItem(EstablishmentId establishmentId, OrderItemId id);

    /** Grava a mudança do item SÓ SE ele ainda estiver no status esperado (o UPDATE condicional). */
    boolean updateItem(OrderItem changed, OrderItemStatus expectedStatus);
}
