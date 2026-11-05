package br.com.cardapio.domain.plan.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o recurso não faz parte do plano (ex.: vídeos no Essencial). */
public class FeatureNotInPlanException extends DomainException {

    public FeatureNotInPlanException(String message) {
        super(message);
    }
}
