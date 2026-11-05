package br.com.cardapio.domain.plan.exception;

import br.com.cardapio.domain.plan.model.PlanResource;
import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o restaurante já usa tudo o que o plano permite deste recurso. */
public class PlanLimitReachedException extends DomainException {

    public PlanLimitReachedException(String planName, PlanResource resource, int max) {
        super("O plano " + planName + " permite até " + resource.count(max) + ". Para ter mais, mude de plano.");
    }
}
