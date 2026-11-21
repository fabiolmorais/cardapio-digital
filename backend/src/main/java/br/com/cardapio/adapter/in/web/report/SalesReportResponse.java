package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.BestDay;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Hour;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Level;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Point;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Totals;
import br.com.cardapio.application.port.in.report.SalesReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "SalesReport", description = "Vendas, com os blocos básicos")
record SalesReportResponse(
        @Schema(requiredMode = REQUIRED) Level level,
        @Schema(requiredMode = REQUIRED) Range period,
        @Schema(requiredMode = REQUIRED) Range comparison,
        @Schema(requiredMode = REQUIRED) Totals current,
        @Schema(requiredMode = REQUIRED) Totals compared,
        @Schema(requiredMode = REQUIRED) Grouping grouping,
        @Schema(requiredMode = REQUIRED) List<Point> series,
        @Schema(requiredMode = REQUIRED, description = "Só as horas com movimento") List<Hour> byHour,
        @Schema(description = "O dia que mais faturou; ausente sem vendas") BestDay bestDay,
        @Schema(requiredMode = REQUIRED) BigDecimal tabsPerDay,
        @Schema(requiredMode = REQUIRED) BigDecimal comparedTabsPerDay,
        @Schema(description = "A hora com mais faturamento médio por dia; ausente sem vendas") Integer peakHour) {

    enum Grouping { DAY, WEEK, MONTH }

    static SalesReportResponse from(SalesReport r) {
        return new SalesReportResponse(Level.valueOf(r.level().name()), Range.of(r.period()), Range.of(r.comparison()),
                Totals.of(r.current()), Totals.of(r.compared()), Grouping.valueOf(r.grouping().name()),
                r.series().stream().map(Point::of).toList(), r.byHour().stream().map(Hour::of).toList(),
                r.bestDay() == null ? null : new BestDay(r.bestDay().day(), r.bestDay().revenue().cents()),
                r.tabsPerDay(), r.comparedTabsPerDay(), r.peakHour());
    }
}
