package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.order.model.ServiceCharge;
import br.com.cardapio.domain.order.model.ServiceChargeSettings;
import br.com.cardapio.domain.order.model.ServiceDestination;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * A taxa de serviço como a tela mostra (RF39): o percentual, se está aceita, quanto dá e o que
 * falta pagar com ela e sem ela, já calculados aqui. A tela só escolhe qual número mostrar (e, com
 * outro valor digitado, soma o valor ao que falta sem a taxa).
 *
 * @param amount        a taxa escolhida (o outro valor, ou o percentual), aceita ou não
 * @param percentAmount o percentual sobre o consumo, para a opção do percentual
 * @param custom        o outro valor escolhido; null quando vale o percentual
 * @param customAllowed o restaurante deixa escolher outro valor
 * @param dueWith       o que falta pagar, com a taxa escolhida
 * @param dueWithout    o que falta pagar, sem a taxa
 * @param waiterName    para quem vai, se for do garçom que atendeu; null na caixinha da equipe (ou se
 *                      ninguém atendeu a mesa ainda)
 */
public record ServiceOffer(int percent, boolean accepted, Money amount, Money percentAmount, Money custom,
                           boolean customAllowed, Money dueWith, Money dueWithout, ServiceDestination destination,
                           String waiterName) {

    public ServiceOffer {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(percentAmount);
        Objects.requireNonNull(dueWith);
        Objects.requireNonNull(dueWithout);
        Objects.requireNonNull(destination);
    }

    /** A oferta sobre este consumo, descontado o que a mesa já pagou; null sem taxa. */
    public static ServiceOffer of(ServiceCharge service, Money consumption, Money paid, ServiceChargeSettings settings,
                                  String waiterName) {
        if (service == null) {
            return null;
        }
        Money amount = service.amountOn(consumption);
        return new ServiceOffer(service.percent(), service.accepted(), amount, service.percentOn(consumption),
                service.custom(), settings.customAllowed(), consumption.plus(amount).minusOrZero(paid),
                consumption.minusOrZero(paid), settings.destination(),
                settings.destination() == ServiceDestination.WAITER ? waiterName : null);
    }
}
