package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.menu.model.ChosenOption;
import br.com.cardapio.domain.menu.model.ItemChoices;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.order.model.OrderId;
import br.com.cardapio.domain.order.model.OrderItem;
import br.com.cardapio.domain.order.model.OrderItemId;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lê e grava as escolhas dos itens do pedido (ADR 0010). Ler é sempre uma consulta a mais para a
 * lista toda de itens (IN), nunca uma por item (sem N+1).
 */
@Component
class OrderItemChoices {

    private final OrderItemOptionJpaRepository options;

    OrderItemChoices(OrderItemOptionJpaRepository options) {
        this.options = options;
    }

    /** Os itens do banco no domínio, cada um com as escolhas dele. */
    List<OrderItem> toDomain(UUID establishmentId, Collection<OrderItemJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ChosenOption>> byItem = options.findByEstablishmentIdAndOrderItemIdInOrderByPositionAsc(
                        establishmentId, entities.stream().map(OrderItemJpaEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(OrderItemOptionJpaEntity::getOrderItemId,
                        Collectors.mapping(OrderItemChoices::toDomain, Collectors.toList())));
        return entities.stream().map(e -> toDomain(e, byItem.getOrDefault(e.getId(), List.of()))).toList();
    }

    /** Grava as escolhas dos itens novos (o tamanho já vai na própria linha do item). */
    void insert(Collection<OrderItem> items) {
        List<OrderItemOptionJpaEntity> rows = new ArrayList<>();
        for (OrderItem item : items) {
            copy(item.establishmentId().value(), item.id().value(), item.choices(), rows);
        }
        if (!rows.isEmpty()) {
            options.saveAll(rows);
        }
    }

    /** A parte cancelada de um item leva as mesmas escolhas, com outro id. */
    void copyTo(OrderItem part) {
        insert(List.of(part));
    }

    private static void copy(UUID establishmentId, UUID itemId, ItemChoices choices,
                             List<OrderItemOptionJpaEntity> into) {
        List<ChosenOption> chosen = choices.options();
        for (int position = 0; position < chosen.size(); position++) {
            ChosenOption o = chosen.get(position);
            into.add(new OrderItemOptionJpaEntity(UUID.randomUUID(), establishmentId, itemId, o.groupId().value(),
                    o.optionId().value(), o.groupTitle(), o.name(), o.splitsItem(), o.quantity(), o.price().cents(),
                    o.charged().cents(), position));
        }
    }

    private static ChosenOption toDomain(OrderItemOptionJpaEntity e) {
        return new ChosenOption(new OptionGroupId(e.getOptionGroupId()), e.getGroupTitle(), e.isSplitsItem(),
                new OptionItemId(e.getOptionItemId()), e.getOptionName(), e.getQuantity(),
                Money.ofCents(e.getPriceCents()), Money.ofCents(e.getChargedCents()));
    }

    static OrderItem toDomain(OrderItemJpaEntity e, List<ChosenOption> chosen) {
        ItemChoices choices = new ItemChoices(
                e.getProductVariantId() == null ? null : new ProductVariantId(e.getProductVariantId()),
                e.getVariantName(), chosen);
        return new OrderItem(new OrderItemId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                new OrderId(e.getOrderId()), new ProductId(e.getProductId()), e.getProductName(),
                Money.ofCents(e.getUnitPriceCents()), e.getQuantity(), e.getNote(),
                OrderItemStatus.valueOf(e.getStatus()), e.getCreatedAt(), e.getPreparingAt(), e.getReadyAt(),
                e.getDeliveredAt(), e.getDeliveredBy() == null ? null : new UserId(e.getDeliveredBy()),
                e.getCanceledAt(), e.getCanceledBy() == null ? null : new UserId(e.getCanceledBy()),
                e.getCancelReason(), e.getTabRodizioId() == null ? null : new TabRodizioId(e.getTabRodizioId()),
                choices);
    }
}
