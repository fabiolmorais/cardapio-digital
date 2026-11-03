package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Como o restaurante recebe pedidos pelo celular.
 *
 * @param orderingEnabled  desligado, a mesa só chama o garçom e vê o cardápio
 * @param approvalRequired todo pedido espera o garçom confirmar antes de ir para a cozinha
 * @param maxAmount        acima deste valor, o pedido espera o garçom; null = sem limite
 * @param maxItems         acima desta quantidade de itens (somando as quantidades), também; null = sem limite
 */
public record OrderSettings(
        OrderSettingsId id,
        EstablishmentId establishmentId,
        boolean orderingEnabled,
        boolean approvalRequired,
        Money maxAmount,
        Integer maxItems,
        Instant updatedAt
) {

    /** Os padrões de um restaurante novo: pedido ligado, direto para a cozinha, com os limites da especificação. */
    public static final Money DEFAULT_MAX_AMOUNT = Money.ofCents(300_00);
    public static final int DEFAULT_MAX_ITEMS = 10;
    public static final int MAX_ITEMS_LIMIT = 100;

    public OrderSettings {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(updatedAt);
        if (maxAmount != null && maxAmount.cents() < 1_00) {
            throw new InvalidValueException("O valor máximo por pedido deve ser de pelo menos R$ 1,00");
        }
        if (maxItems != null && (maxItems < 1 || maxItems > MAX_ITEMS_LIMIT)) {
            throw new InvalidValueException("A quantidade máxima por pedido deve ser de 1 a " + MAX_ITEMS_LIMIT);
        }
    }

    public static OrderSettings defaults(EstablishmentId establishmentId, Instant now) {
        return new OrderSettings(OrderSettingsId.newId(), establishmentId, true, false, DEFAULT_MAX_AMOUNT,
                DEFAULT_MAX_ITEMS, now);
    }

    public OrderSettings update(boolean newOrderingEnabled, boolean newApprovalRequired, Money newMaxAmount,
                                Integer newMaxItems, Instant now) {
        return new OrderSettings(id, establishmentId, newOrderingEnabled, newApprovalRequired, newMaxAmount,
                newMaxItems, now);
    }

    /**
     * O pedido espera o garçom? Sim, se a confirmação estiver ligada para todos, ou se ele passar
     * de um dos limites (a especificação: "um pedido acima de R$ 300 ou de 10 itens para na
     * confirmação do garçom").
     */
    public boolean needsApproval(Money total, int items) {
        return approvalRequired
                || (maxAmount != null && total.compareTo(maxAmount) > 0)
                || (maxItems != null && items > maxItems);
    }
}
