package br.com.cardapio.domain.report.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 403: o plano do restaurante não tem os relatórios pedidos (ou a exportação). */
public class ReportsNotInPlanException extends DomainException {

    public ReportsNotInPlanException(String message) {
        super(message);
    }
}
