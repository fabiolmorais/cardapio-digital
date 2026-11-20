package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.order.model.ServiceCharge;
import br.com.cardapio.domain.order.model.ServiceDestination;
import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * A taxa de serviço como a tela mostra (RF39): o percentual, se está aceita, quanto dá e o que
 * falta pagar com ela e sem ela, já calculados aqui. A tela só escolhe qual número mostrar.
 *
 * @param amount      a taxa sobre o consumo (aceita ou não)
 * @param dueWith     o que falta pagar, com a taxa
 * @param dueWithout  o que falta pagar, sem a taxa
 * @param waiterName  para quem vai, se for do garçom que atendeu; null na caixinha da equipe (ou se
 *                    ninguém atendeu a mesa ainda)
 */
public record ServiceOffer(int percent, boolean accepted, Money amount, Money dueWith, Money dueWithout,
                           ServiceDestination destination, String waiterName) {

    public ServiceOffer {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(dueWith);
        Objects.requireNonNull(dueWithout);
        Objects.requireNonNull(destination);
    }

    /** A oferta sobre este consumo, descontado o que a mesa já pagou; null sem taxa. */
    public static ServiceOffer of(ServiceCharge service, Money consumption, Money paid, ServiceDestination destination,
                                  String waiterName) {
        if (service == null) {
            return null;
        }
        Money amount = service.amountOn(consumption);
        return new ServiceOffer(service.percent(), service.accepted(), amount,
                consumption.plus(amount).minusOrZero(paid), consumption.minusOrZero(paid), destination,
                destination == ServiceDestination.WAITER ? waiterName : null);
    }
}
