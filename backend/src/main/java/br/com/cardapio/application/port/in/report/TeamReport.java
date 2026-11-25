package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.CallTotals;
import br.com.cardapio.domain.report.model.CallTypeCount;
import br.com.cardapio.domain.report.model.DuplicateWarnings;
import br.com.cardapio.domain.report.model.HourDuration;
import br.com.cardapio.domain.report.model.RatingPoint;
import br.com.cardapio.domain.report.model.RatingStats;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.ShiftLoad;
import br.com.cardapio.domain.report.model.TableCalls;
import br.com.cardapio.domain.report.model.WaiterStats;

import java.util.List;
import java.util.Optional;

/**
 * Atendimento e equipe (desenho 14.5, REL-38 a 53): só no Business.
 *
 * @param tableAverage os chamados por comanda do restaurante todo; vazio sem comandas
 * @param ratings      as avaliações do atendimento no período (REL-83, 84)
 * @param comparedRatingAverage a nota média no período de comparação; null sem avaliações
 * @param ratingTrend  a nota ao longo do período, por dia ou por semana
 */
public record TeamReport(ReportPeriod period, ReportPeriod comparison, CallTotals current, CallTotals compared,
                         List<WaiterStats> waiters, List<HourDuration> responseByHour, List<CallTypeCount> byType,
                         List<TableCalls> topTables, Optional<Double> tableAverage, DuplicateWarnings warnings,
                         List<ShiftLoad> busiestShifts, RatingStats ratings, Double comparedRatingAverage,
                         List<RatingPoint> ratingTrend) {
}
