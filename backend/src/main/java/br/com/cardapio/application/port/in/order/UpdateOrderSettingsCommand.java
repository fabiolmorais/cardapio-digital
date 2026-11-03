package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * @param maxAmount null para não ter limite de valor
 * @param maxItems  null para não ter limite de itens
 */
public record UpdateOrderSettingsCommand(EstablishmentId establishmentId, boolean orderingEnabled,
                                         boolean approvalRequired, Money maxAmount, Integer maxItems) {

    public UpdateOrderSettingsCommand {
        Objects.requireNonNull(establishmentId);
    }
}
