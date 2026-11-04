package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.service.RoundPolicy;

import java.time.Instant;
import java.util.List;

/** A política das rodadas a partir dos pedidos da comanda: o que ainda está a caminho e a última rodada. */
public final class RodizioRounds {

    private RodizioRounds() {
    }

    /** As rodadas recusadas pelo garçom não contam: nem como "a caminho", nem para o intervalo. */
    public static RoundPolicy policy(RodizioRules rules, TabRodizio rodizio, List<OrderWithItems> orders) {
        List<OrderWithItems> rounds = orders.stream()
                .filter(o -> o.order().status() != OrderStatus.REJECTED)
                .filter(o -> o.items().stream().anyMatch(OrderItem::inRodizio))
                .toList();
        int undelivered = rounds.stream()
                .flatMap(o -> o.items().stream())
                .filter(i -> i.inRodizio() && i.status().isOnTheWay())
                .mapToInt(OrderItem::quantity)
                .sum();
        Instant last = rounds.stream().map(o -> o.order().createdAt()).max(Instant::compareTo).orElse(null);
        return new RoundPolicy(rules, rodizio, undelivered, last);
    }
}
