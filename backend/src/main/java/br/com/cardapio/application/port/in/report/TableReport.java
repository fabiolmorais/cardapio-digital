package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.ForgottenTabs;
import br.com.cardapio.domain.report.model.Occupancy;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.StayTotals;
import br.com.cardapio.domain.report.model.TableMoves;
import br.com.cardapio.domain.report.model.TableUsage;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;
import java.util.Optional;

/**
 * Mesas e ocupação (desenho 14.6, REL-56 a 62): só no Business.
 *
 * @param turnover        comandas por mesa por dia; vazio sem mesas
 * @param perOccupiedHour o faturamento por hora de mesa ocupada; vazio sem comandas
 * @param tables          as mesas ativas, para o mapa do salão
 */
public record TableReport(ReportPeriod period, ReportPeriod comparison, StayTotals current, StayTotals compared,
                          Optional<Double> turnover, Optional<Double> comparedTurnover,
                          Optional<Money> perOccupiedHour, Optional<Money> comparedPerOccupiedHour,
                          List<TableUsage> tables, Occupancy occupancy, ForgottenTabs forgotten, TableMoves moves) {
}
