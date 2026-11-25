package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.NetworkTotals;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.report.model.UnitPerformance;

import java.util.List;

/**
 * A comparação entre unidades (REL-80).
 *
 * @param period     o período no fuso da unidade de agora (o título da tela)
 * @param comparison o período anterior, do mesmo tamanho
 * @param grouping   como a evolução agrupa os dias
 * @param units      as unidades, da que mais faturou para a que menos faturou
 * @param network    a rede inteira
 */
public record UnitComparison(ReportPeriod period, ReportPeriod comparison, SeriesGrouping grouping,
                             List<UnitPerformance> units, NetworkTotals network) {

    public UnitComparison {
        units = List.copyOf(units);
    }
}
