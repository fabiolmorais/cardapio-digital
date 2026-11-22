package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.OperationTotals;
import br.com.cardapio.domain.report.model.TodayForecast;
import br.com.cardapio.domain.report.model.WaiterCalls;

import java.util.List;
import java.util.Optional;

/**
 * Os blocos completos da Visão geral (Business): os números de operação com a comparação, "Hoje, até
 * agora", os garçons e os pontos de atenção.
 *
 * @param today vazio sem histórico do mesmo dia da semana
 */
public record OverviewFull(OperationTotals current, OperationTotals compared, Optional<TodayForecast> today,
                           List<WaiterCalls> waiters, List<AttentionPoint> attention) {
}
