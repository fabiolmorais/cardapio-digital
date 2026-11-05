package br.com.cardapio.domain.plan.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

/** 404: não há plano ativo com este código. */
public class PlanNotFoundException extends NotFoundException {

    public PlanNotFoundException() {
        super("Plano não encontrado.");
    }
}
