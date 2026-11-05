package br.com.cardapio.application.port.out.order;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /** Grava a mudança do pedido SÓ SE ele ainda estiver no status esperado (dois garçons respondendo juntos). */
    boolean updateOrder(Order changed, OrderStatus expectedStatus);

    /** Cancela, de uma vez, os itens ainda a caminho de um pedido (o pedido recusado), com o motivo. */
    void cancelItemsOnTheWay(EstablishmentId establishmentId, OrderId orderId, UserId by, String reason,
                             Instant now);

    /**
     * A fila da cozinha: os itens nestes status, dos pedidos que foram para a cozinha (os parados
     * na confirmação ficam de fora), por ordem de chegada. Os cortes do rodízio de passagem não
     * entram: quem leva é o passador, e não a cozinha.
     */
    ResultPage<KitchenItemView> findQueue(EstablishmentId establishmentId, Set<OrderItemStatus> statuses,
                                          PageQuery page);

    /** A fila dos passadores: os cortes pedidos no rodízio de passagem, ainda a caminho, por ordem de chegada. */
    ResultPage<KitchenItemView> findPassagemQueue(EstablishmentId establishmentId, PageQuery page);

    /** Os pedidos parados na confirmação, com os itens: duas consultas, sem N+1. */
    ResultPage<WaitingOrderView> findWaitingApproval(EstablishmentId establishmentId, PageQuery page);
}
