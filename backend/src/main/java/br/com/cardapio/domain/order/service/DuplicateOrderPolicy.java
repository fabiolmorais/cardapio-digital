package br.com.cardapio.domain.order.service;

import br.com.cardapio.domain.order.model.Order;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * O que conta como "já pedido" para a trava de duplicados (ADR 0011): o cliente pediu a um garçom e
 * repetiu a outro, ou pediu pelo celular e também ao garçom.
 */
public final class DuplicateOrderPolicy {

    /** Por este tempo o item conta mesmo já entregue: cobre o que sai rápido, como o chopp. */
    public static final Duration RECENT = Duration.ofMinutes(5);

    private DuplicateOrderPolicy() {
    }

    /**
     * Conta o item que ainda não foi entregue, ou que foi pedido há menos de {@link #RECENT}. Pedido
     * recusado e item cancelado não contam; as rodadas do rodízio também não, porque repetir é o normal.
     */
    public static boolean counts(Order order, OrderItem item, Instant now) {
        if (order.status() == OrderStatus.REJECTED || item.status() == OrderItemStatus.CANCELED || item.inRodizio()) {
            return false;
        }
        return item.status().isOnTheWay() || item.createdAt().isAfter(now.minus(RECENT));
    }
}
