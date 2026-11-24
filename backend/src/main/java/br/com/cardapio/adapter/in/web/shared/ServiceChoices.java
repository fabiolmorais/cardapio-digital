package br.com.cardapio.adapter.in.web.shared;

import br.com.cardapio.domain.order.model.ServiceChoice;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

/**
 * Os dois campos da taxa de serviço nos pedidos (RF39), na escolha do domínio: com outro valor, vale
 * o valor (com o serviço aceito); só com o "aceita", o percentual ou sem serviço; sem nenhum dos
 * dois, null (fica como estava).
 */
public final class ServiceChoices {

    private ServiceChoices() {
    }

    public static ServiceChoice of(Boolean accepted, Long customCents) {
        if (customCents != null) {
            if (Boolean.FALSE.equals(accepted)) {
                throw new InvalidValueException("Outro valor de serviço só com o serviço aceito");
            }
            return ServiceChoice.custom(Money.ofCents(customCents));
        }
        return accepted == null ? null : ServiceChoice.of(accepted);
    }
}
