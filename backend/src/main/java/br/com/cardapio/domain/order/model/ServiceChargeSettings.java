package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * A taxa de serviço do restaurante (RF39): se cobra, o percentual, para quem fica registrada e se a
 * mesa pode escolher outro valor no lugar do percentual. Começa desligada, em 10%, para a caixinha
 * da equipe e sem outro valor.
 */
public record ServiceChargeSettings(boolean enabled, int percent, ServiceDestination destination,
                                    boolean customAllowed) {

    public static final int DEFAULT_PERCENT = 10;
    public static final int MAX_PERCENT = 20;
    public static final ServiceChargeSettings DEFAULT =
            new ServiceChargeSettings(false, DEFAULT_PERCENT, ServiceDestination.TEAM, false);

    public ServiceChargeSettings {
        Objects.requireNonNull(destination);
        if (percent < 1 || percent > MAX_PERCENT) {
            throw new InvalidValueException("A taxa de serviço vai de 1% a " + MAX_PERCENT + "%");
        }
    }

    public ServiceChargeSettings(boolean enabled, int percent, ServiceDestination destination) {
        this(enabled, percent, destination, false);
    }
}
