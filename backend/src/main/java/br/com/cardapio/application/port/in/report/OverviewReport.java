package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.DayComparison;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PaymentTotal;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.WeekdayHourAverage;

import java.util.List;

/**
 * A Visão geral (desenho 14.1): os blocos básicos (REL-01, 02, 03, 05, 08, 13) e, no Business, os
 * completos.
 *
 * @param comparison o período da comparação (mesmo tamanho antes; um dia só: o mesmo dia da semana anterior)
 * @param compared   os números da comparação; em "hoje", até a mesma hora
 * @param full       os blocos completos; null para quem só tem os básicos (o dado não sai do servidor)
 */
public record OverviewReport(ReportLevel level, ReportPeriod period, ReportPeriod comparison,
                             ReportTotals current, ReportTotals compared, List<DayComparison> byDay,
                             List<PaymentTotal> payments, List<WeekdayHourAverage> heatmap,
                             List<ItemSales> topItems, OverviewFull full) {
}
