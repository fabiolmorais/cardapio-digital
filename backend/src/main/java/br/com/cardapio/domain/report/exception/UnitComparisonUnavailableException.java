package br.com.cardapio.domain.report.exception;

import br.com.cardapio.domain.shared.exception.NotFoundException;

/**
 * 404: a comparação entre unidades não existe para esta pessoa (plano sem várias unidades, uma unidade
 * só, ou alguma unidade da empresa da qual ela não é dona). Não revela quais unidades existem.
 */
public class UnitComparisonUnavailableException extends NotFoundException {

    public UnitComparisonUnavailableException() {
        super("Comparação entre unidades não encontrada");
    }
}
