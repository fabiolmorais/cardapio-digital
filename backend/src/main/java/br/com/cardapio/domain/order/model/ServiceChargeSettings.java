package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;

/**
 * A taxa de serviço do restaurante (RF39): se cobra, o percentual e para quem fica registrada.
 * Começa desligada, em 10%, para a caixinha da equipe.
 */
public record ServiceChargeSettings(boolean enabled, int percent, ServiceDestination destination) {

    public static final int DEFAULT_PERCENT = 10;
    public static final int MAX_PERCENT = 20;
    public static final ServiceChargeSettings DEFAULT =
            new ServiceChargeSettings(false, DEFAULT_PERCENT, ServiceDestination.TEAM);

    public ServiceChargeSettings {
        Objects.requireNonNull(destination);
        if (percent < 1 || percent > MAX_PERCENT) {
            throw new InvalidValueException("A taxa de serviço vai de 1% a " + MAX_PERCENT + "%");
        }
    }
}
