package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.out.menu.ProductOptionsPort;
import br.com.cardapio.application.port.out.menu.ProductPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.PricedSelection;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductOptions;
import br.com.cardapio.domain.order.exception.ProductUnavailableException;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderLine;
import br.com.cardapio.domain.rodizio.exception.ProductOnlyInRodizioException;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * As linhas de um envio conferidas contra o cardápio de agora: o mesmo caminho para o pedido do
 * celular e o da equipe. O nome e o preço vêm daqui, nunca de quem enviou.
 */
final class OrderDraft {

    /** Linhas diferentes num envio (cada uma com até 20 unidades). */
    static final int MAX_LINES = 30;

    private final EstablishmentId establishmentId;
    private final List<OrderLine> lines;
    private final Map<ProductId, Product> menu;

    private OrderDraft(EstablishmentId establishmentId, List<OrderLine> lines, Map<ProductId, Product> menu) {
        this.establishmentId = establishmentId;
        this.lines = lines;
        this.menu = menu;
    }

    static void requireValidSize(List<OrderLine> lines) {
        if (lines.isEmpty() || lines.size() > MAX_LINES) {
            throw new InvalidValueException("O pedido deve ter de 1 a " + MAX_LINES + " itens diferentes");
        }
    }

    /** O cardápio de agora, numa consulta só: o que saiu ou esgotou volta para quem enviou. */
    static OrderDraft of(ProductPort products, EstablishmentId establishmentId, List<OrderLine> lines) {
        Map<ProductId, Product> menu = products.findActiveByIds(establishmentId,
                        lines.stream().map(OrderLine::productId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        for (OrderLine line : lines) {
            Product product = menu.get(line.productId());
            if (product == null) {
                throw new ProductUnavailableException(null);
            }
            if (!product.available()) {
                throw new ProductUnavailableException(product.details().name());
            }
        }
        return new OrderDraft(establishmentId, lines, menu);
    }

    Map<ProductId, Product> menu() {
        return menu;
    }

    List<OrderLine> lines() {
        return lines;
    }

    Set<ProductId> productIds() {
        return menu.keySet();
    }

    /** Fora de uma rodada, não entra o que é só do rodízio. */
    void requireALaCarte() {
        menu.values().stream().filter(p -> !p.details().aLaCarte()).findFirst().ifPresent(p -> {
            throw new ProductOnlyInRodizioException(p.details().name());
        });
    }

    /** O preço de cada linha com o tamanho e as opções (ADR 0010), conferido contra o cardápio de agora. */
    Priced priced(ProductOptionsPort productOptions, boolean inRodizio) {
        Map<ProductId, ProductOptions> configured = productOptions.findFor(establishmentId, menu.keySet());
        return new Priced(lines.stream()
                .map(line -> configured.getOrDefault(line.productId(), ProductOptions.none(line.productId()))
                        .price(menu.get(line.productId()).details(), line.selection(), inRodizio))
                .toList());
    }

    final class Priced {

        private final List<PricedSelection> selections;

        private Priced(List<PricedSelection> selections) {
            this.selections = selections;
        }

        Money total() {
            Money total = Money.ZERO;
            for (int i = 0; i < lines.size(); i++) {
                total = total.plus(selections.get(i).unitPrice().times(lines.get(i).quantity()));
            }
            return total;
        }

        int count() {
            return lines.stream().mapToInt(OrderLine::quantity).sum();
        }

        /** Os itens do pedido; com o rodízio, entram como rodada (a preço zero). */
        List<OrderItem> items(OrderId orderId, TabRodizio rodizio, Instant now) {
            List<OrderItem> items = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                OrderLine line = lines.get(i);
                Product product = menu.get(line.productId());
                items.add(rodizio != null
                        ? OrderItem.inRound(orderId, product, line, rodizio.id(), selections.get(i).choices(), now)
                        : OrderItem.from(orderId, product, line, selections.get(i), now));
            }
            return items;
        }
    }
}
