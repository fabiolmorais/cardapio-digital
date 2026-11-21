package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.DailyRevenue;
import br.com.cardapio.domain.report.model.HourAverage;
import br.com.cardapio.domain.report.model.ReportLevel;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import br.com.cardapio.domain.report.model.SeriesPoint;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vendas (desenho 14.2), só com os blocos básicos (REL-01, 03, 05, 08).
 *
 * @param bestDay   o dia que mais faturou; null sem vendas
 * @param peakHour  a hora com mais faturamento médio por dia; null sem vendas
 */
public record SalesReport(ReportLevel level, ReportPeriod period, ReportPeriod comparison,
                          ReportTotals current, ReportTotals compared, SeriesGrouping grouping,
                          List<SeriesPoint> series, List<HourAverage> byHour, DailyRevenue bestDay,
                          BigDecimal tabsPerDay, BigDecimal comparedTabsPerDay, Integer peakHour) {
}
