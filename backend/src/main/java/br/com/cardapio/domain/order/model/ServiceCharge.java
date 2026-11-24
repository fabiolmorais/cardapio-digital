package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

/**
 * A taxa de serviço de uma comanda: o percentual copiado quando ela foi oferecida, se a mesa
 * aceitou e, se o restaurante permite, um valor escolhido no lugar do percentual ("outro valor").
 * É opcional (Lei 13.419/2017): o cliente tira ou troca ao pedir a conta, e o garçom troca na hora
 * de receber, se o cliente mudar de ideia.
 *
 * @param custom o "outro valor", no lugar do percentual; null quando vale o percentual. Só existe
 *               com a taxa aceita
 */
public record ServiceCharge(int percent, boolean accepted, Money custom) {

    /** O maior "outro valor": R$ 1.000,00, para um dígito a mais não virar uma gorjeta enorme. */
    public static final Money MAX_CUSTOM = Money.ofCents(100_000);

    public ServiceCharge {
        if (percent < 1 || percent > ServiceChargeSettings.MAX_PERCENT) {
            throw new InvalidValueException("A taxa de serviço vai de 1% a " + ServiceChargeSettings.MAX_PERCENT + "%");
        }
        if (custom != null) {
            if (!accepted) {
                throw new InvalidValueException("Outro valor de serviço só com o serviço aceito");
            }
            if (custom.isZero() || custom.compareTo(MAX_CUSTOM) > 0) {
                throw new InvalidValueException("O outro valor de serviço vai de R$ 0,01 a R$ 1.000,00");
            }
        }
    }

    public ServiceCharge(int percent, boolean accepted) {
        this(percent, accepted, null);
    }

    /** O percentual sobre este consumo, arredondado no centavo (para mostrar, escolhido ou não). */
    public Money percentOn(Money consumption) {
        return Money.ofCents((consumption.cents() * percent + 50) / 100);
    }

    /** A taxa sobre este consumo, aceita ou não (para mostrar o valor): o outro valor, ou o percentual. */
    public Money amountOn(Money consumption) {
        return custom != null ? custom : percentOn(consumption);
    }

    /** O que entra na conta: a taxa, se a mesa aceitou; senão, zero. */
    public Money chargedOn(Money consumption) {
        return accepted ? amountOn(consumption) : Money.ZERO;
    }

    /** Aceita ou tira o percentual; volta ao percentual, se havia outro valor. */
    public ServiceCharge accept(boolean newAccepted) {
        return new ServiceCharge(percent, newAccepted, null);
    }

    /** Aplica a escolha: o percentual, sem serviço ou outro valor. */
    public ServiceCharge choose(ServiceChoice choice) {
        return new ServiceCharge(percent, choice.accepted(), choice.custom());
    }
}
