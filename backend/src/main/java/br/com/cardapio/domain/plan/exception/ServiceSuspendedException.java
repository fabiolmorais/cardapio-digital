package br.com.cardapio.domain.plan.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * 422: a assinatura passou da tolerância, e a mesa não chama nem pede pelo celular. A frase é
 * para o cliente da mesa: ele não sabe (nem precisa saber) de assinatura.
 */
public class ServiceSuspendedException extends DomainException {

    public ServiceSuspendedException() {
        super("O atendimento pelo celular está pausado neste restaurante. Chame um garçom.");
    }
}
