package br.com.cardapio.domain.report.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o período pedido passa do histórico do plano ou entra no futuro. */
public class ReportPeriodOutOfRangeException extends DomainException {

    public ReportPeriodOutOfRangeException(String message) {
        super(message);
    }
}
