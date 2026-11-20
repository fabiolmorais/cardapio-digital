package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

/**
 * A taxa de serviço de uma comanda: o percentual copiado quando ela foi oferecida e se a mesa
 * aceitou. É opcional (Lei 13.419/2017): o cliente tira ao pedir a conta, e o garçom troca na hora
 * de receber, se o cliente mudar de ideia.
 */
public record ServiceCharge(int percent, boolean accepted) {

    public ServiceCharge {
        if (percent < 1 || percent > ServiceChargeSettings.MAX_PERCENT) {
            throw new InvalidValueException("A taxa de serviço vai de 1% a " + ServiceChargeSettings.MAX_PERCENT + "%");
        }
    }

    /** A taxa sobre este consumo, aceita ou não (para mostrar o valor), arredondada no centavo. */
    public Money amountOn(Money consumption) {
        return Money.ofCents((consumption.cents() * percent + 50) / 100);
    }

    /** O que entra na conta: a taxa, se a mesa aceitou; senão, zero. */
    public Money chargedOn(Money consumption) {
        return accepted ? amountOn(consumption) : Money.ZERO;
    }

    public ServiceCharge accept(boolean newAccepted) {
        return new ServiceCharge(percent, newAccepted);
    }
}
