package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

/**
 * O que a mesa (ao pedir a conta) ou o garçom (ao receber e ao fechar) escolheu para a taxa de
 * serviço: o percentual do restaurante, sem serviço, ou outro valor (quando o restaurante permite).
 *
 * @param custom o outro valor; null para o percentual ou sem serviço
 */
public record ServiceChoice(boolean accepted, Money custom) {

    public static final ServiceChoice PERCENT = new ServiceChoice(true, null);
    public static final ServiceChoice NONE = new ServiceChoice(false, null);

    public ServiceChoice {
        if (custom != null && !accepted) {
            throw new InvalidValueException("Outro valor de serviço só com o serviço aceito");
        }
    }

    public static ServiceChoice of(boolean accepted) {
        return accepted ? PERCENT : NONE;
    }

    public static ServiceChoice custom(Money amount) {
        return new ServiceChoice(true, amount);
    }

    public boolean isCustom() {
        return custom != null;
    }
}
