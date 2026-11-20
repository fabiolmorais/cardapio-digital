package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;
import br.com.cardapio.domain.order.model.ServiceChargeSettings;

/**
 * @param staffOrderingEnabled a equipe pode lançar pedidos; null mantém como está
 * @param maxAmount null para não ter limite de valor
 * @param maxItems  null para não ter limite de itens
 */
public record UpdateOrderSettingsCommand(EstablishmentId establishmentId, boolean orderingEnabled,
                                         boolean approvalRequired, Boolean staffOrderingEnabled,
                                         Money maxAmount, Integer maxItems, ServiceChargeSettings serviceCharge) {

    public UpdateOrderSettingsCommand {
        Objects.requireNonNull(establishmentId);
    }

    /** Sem a taxa de serviço: ela continua como está. */
    public UpdateOrderSettingsCommand(EstablishmentId establishmentId, boolean orderingEnabled,
                                      boolean approvalRequired, Boolean staffOrderingEnabled, Money maxAmount,
                                      Integer maxItems) {
        this(establishmentId, orderingEnabled, approvalRequired, staffOrderingEnabled, maxAmount, maxItems, null);
    }
}
