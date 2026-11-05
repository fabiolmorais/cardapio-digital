package br.com.cardapio.domain.plan.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o plano é sob consulta (Enterprise) e não se escolhe pela tela. */
public class PlanNotSelfServiceException extends DomainException {

    public PlanNotSelfServiceException(String planName) {
        super("O plano " + planName + " é sob consulta. Fale com a gente para combinar.");
    }
}
