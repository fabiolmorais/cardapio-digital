package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.out.order.OrderWithItems;
import br.com.cardapio.application.port.out.rodizio.RodizioPort;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.rodizio.exception.ProductNotInRodizioException;
import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.rodizio.service.RoundPolicy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A política das rodadas a partir dos pedidos da comanda: o que ainda está a caminho e a última rodada. */
public final class RodizioRounds {

    private RodizioRounds() {
    }

    /**
     * Confere a rodada: só os produtos incluídos no rodízio e dentro das regras que o restaurante
     * ligou. Os produtos e as regras vêm do cadastro de agora: o que saiu do rodízio sai da rodada.
     */
    static void requireAllowed(RodizioPort rodizios, OrderPort orders, Tab tab, TabRodizio rodizio,
                               Map<ProductId, Product> menu, List<OrderLine> lines, Instant now) {
        Rodizio registered = rodizios.find(tab.establishmentId(), rodizio.rodizioId()).orElse(null);
        Set<ProductId> included = registered == null ? Set.of() : registered.productIds();
        menu.values().stream().filter(p -> !included.contains(p.id())).findFirst().ifPresent(p -> {
            throw new ProductNotInRodizioException(p.details().name());
        });
        RodizioRules rules = registered == null ? RodizioRules.NONE : registered.rules();
        policy(rules, rodizio, orders.findByTab(tab.establishmentId(), tab.id())).check(
                lines.stream().map(l -> new RoundPolicy.Line(menu.get(l.productId()).details().name(),
                        l.quantity())).toList(), now);
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
